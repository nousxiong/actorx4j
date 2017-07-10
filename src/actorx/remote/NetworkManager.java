/**
 * 
 */
package actorx.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import cque.ConcurrentObjectPool;
import cque.Node;
import cque.util.NodeFactory;
import cque.util.PoolGuard;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.AtomCode;
import actorx.IFiberActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.remote.mina.MinaTcpClient;
import actorx.remote.mina.MinaTcpServer;
import actorx.util.ConcurrentHashMap;

/**
 * @author Xiong
 * 网络管理器
 */
public class NetworkManager implements IFiberActorHandler {
	private ActorSystem axSys;
	private ConcurrentObjectPool<Node<Object>> cpool;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private FiberScheduler fibSche = new FiberExecutorScheduler("NetworkFibsche", executor);
	private ActorId lisnAid;
	private ConcurrentHashMap<Long, NetSession> sessions = new ConcurrentHashMap<Long, NetSession>();
	private Actor selfAx;

	private ActorRelationMap relations = new ActorRelationMap();

	public NetworkManager(ActorSystem axSys, int poolSize, int initSize, int maxSize){
		this.axSys = axSys;
		this.cpool = new ConcurrentObjectPool<Node<Object>>(new NodeFactory<Object>(), poolSize, initSize, maxSize);
	}

	@Suspendable
	public void start(){
		this.selfAx = axSys.spawn();
	}

	@Suspendable
	public void listen(LocalInfo localInfo){
		if (lisnAid == null){
			lisnAid = axSys.spawn(selfAx, fibSche, this, LinkType.MONITORED);
			selfAx.send(lisnAid, "INIT", true);
			selfAx.send(lisnAid, "LISTEN", localInfo);
		}
	}
	
	@Suspendable
	public void sendRemote(ActorId toAid, Message rmsg){
		NetSession netSession = sessions.get(toAid.axid);
		if (netSession == null){
			if (rmsg != null){
				rmsg.release();
			}
//			throw new RuntimeException("Remote actor can't reach");
		}else{
			selfAx.send(netSession.getHandleAid(), "REMOTE", toAid, rmsg, netSession);
		}
	}

	@Suspendable
	public void addRemote(long axid, RemoteInfo remoteInfo){
		NetSession netSession = addNetSession(axid, new NetSession());
		if (netSession != null){
			selfAx.send(netSession.getHandleAid(), "INIT", false, remoteInfo);
		}
	}

	@Suspendable
	public boolean removeRemote(long axid){
		NetSession netSession = removeNetSession(axid);
		if (netSession != null){
			ActorId handleAid = netSession.getHandleAid();
			netSession.close();
			if (!ActorId.equals(handleAid, lisnAid)){
				selfAx.send(handleAid, "STOP");
				try {
					selfAx.recvExit(handleAid);
				} catch (InterruptedException e) {
				}
			}
			return true;
		}
		return false;
	}
	
	@Suspendable
	public NetSession addNetSession(long axid, NetSession netSession){
		ActorId handleAid = netSession.getHandleAid();
		if (handleAid == null){
			handleAid = axSys.spawn(selfAx, fibSche, this, LinkType.MONITORED);
		}
		netSession.setHandleAid(handleAid);
		if (sessions.putIfAbsent(axid, netSession) != null){
			selfAx.send(handleAid, "STOP");
			try {
				selfAx.recvExit(handleAid);
			} catch (InterruptedException e) {
			}
			return null;
		}
		return netSession;
	}

	@Suspendable
	public boolean hasNetSession(long axid){
		return sessions.containsKey(axid);
	}

	@Suspendable
	public NetSession removeNetSession(long axid){
		return sessions.remove(axid);
	}
	
	@Suspendable
	public void stop(){
		if (lisnAid != null){
			selfAx.send(lisnAid, "STOP");
		}
		List<ActorId> contAids = new ArrayList<ActorId>(sessions.size());
		for (NetSession netSession : sessions.values()){
			ActorId handleAid = netSession.getHandleAid();
			if (!ActorId.equals(handleAid, lisnAid)){
				contAids.add(handleAid);
				selfAx.send(handleAid, "STOP");
			}
		}
		
		for (ActorId contAid : contAids){
			try {
				selfAx.recvExit(contAid);
			} catch (InterruptedException e) {
			}
		}
		if (lisnAid != null){
			try {
				selfAx.recvExit(lisnAid);
			} catch (InterruptedException e) {
			}
		}
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
	}

	@Override
	public void run(Actor self) throws SuspendExecution, Exception {
		Message cmsg = self.recv("INIT", "STOP");
		if (AtomCode.equals(cmsg.getType(), "STOP")){
			return;
		}
		
		boolean isSrv = cmsg.getBool();
		if (isSrv){
			server(self);
		}else{
			RemoteInfo remoteInfo = cmsg.getRaw();
			client(self, remoteInfo);
		}
	}
	
	private void server(Actor self) throws SuspendExecution, Exception {
		Pattern pattern = new Pattern();
		pattern.match("LISTEN", "REMOTE", "STOP");
		
		INetworkServerService srv = null;
		boolean quit = false;
		while (!quit){
			try (PoolGuard guard = self.precv(pattern)){
				Message msg = guard.get();
				String type = msg.getType();
				switch (type){
				case "LISTEN":{
					if (srv == null){
						LocalInfo localInfo = msg.getRaw();
						switch (localInfo.getProtocolType()){
						case MINA_TCP:
							srv = new MinaTcpServer(self, cpool, relations, localInfo, pattern);
							srv.start();
							break;
						default:
							break;
						}
					}
				}break;
				case "REMOTE":{
					ActorId toAid = msg.getRaw();
					Message rmsg = msg.getRaw();
					NetSession netSession = msg.getRaw();
					srv.sendRemote(toAid, rmsg, netSession);
				}break;
				case "STOP":{
					if (srv != null){
						srv.stop();
					}
					quit = true;
				}break;
				default:
					if (srv != null){
						srv.handleMessage(msg);
					}
					break;
				}
			}catch (Throwable ex){
				if (srv != null){
					srv.handleException(ex);
				}
			}
		}
	}
	
	private void client(Actor self, RemoteInfo remoteInfo) throws SuspendExecution, Exception {
		Pattern pattern = new Pattern();
		pattern.match("REMOTE", "STOP");
		
		INetworkClientService cln = null;
		switch (remoteInfo.getProtocolType()){
		case MINA_TCP:
			cln = new MinaTcpClient(self, cpool, relations, remoteInfo, pattern);
			cln.start();
			break;
		default:
			break;
		}
		
		boolean quit = false;
		while (!quit){
			try (PoolGuard guard = self.precv(pattern)){
				Message msg = guard.get();
				String type = msg.getType();
				switch (type){
				case "REMOTE":{
					ActorId toAid = msg.getRaw();
					Message rmsg = msg.getRaw();
					cln.sendRemote(toAid, rmsg);
				}break;
				case "STOP":{
					cln.stop();
					quit = true;
				}break;
				default:
					cln.handleMessage(msg);
					break;
				}
			}catch (Throwable ex){
				cln.handleException(ex);
			}
		}
	}

}
