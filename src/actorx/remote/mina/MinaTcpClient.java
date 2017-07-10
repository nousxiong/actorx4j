/**
 * 
 */
package actorx.remote.mina;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.AtomCode;
import actorx.IFiberActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.addon.mina.MinaAtomCode;
import actorx.addon.mina.MinaIoHandler;
import actorx.remote.ActorRelationMap;
import actorx.remote.ErrorCode;
import actorx.remote.INetworkClientService;
import actorx.remote.RemoteInfo;
import actorx.remote.mina.io.AbstractMinaMessage;
import actorx.remote.mina.io.AbstractMinaWrapMessage;
import actorx.remote.mina.io.MinaCodecFactory;
import actorx.remote.mina.io.MinaIoFutureListener;
import actorx.remote.mina.io.MinaMessageFactory;
import actorx.remote.mina.io.MinaMsgCode;
import actorx.remote.mina.io.push.MinaPushMessage;
import actorx.remote.mina.io.request.MinaRequestMessage;
import actorx.remote.mina.io.response.MinaRelationResponse;
import actorx.remote.mina.io.response.MinaResponseMessage;
import actorx.util.ExceptionUtils;
import amina.codec.PooledProtocolCodecFilter;
import amina.session.PooledIoSessionDataStructureFactory;
import amina.transport.socket.nio.PooledNioSocketConnector;
import cque.ConcurrentObjectPool;
import cque.IObjectFactory;
import cque.IPooledObject;
import cque.LinkedQueue;
import cque.Node;
import cque.SingleObjectPool;
import cque.util.PoolGuard;

/**
 * @author Xiong
 *
 */
public class MinaTcpClient extends AbstractMinaService implements INetworkClientService {
	private static final Logger logger = LoggerFactory.getLogger(MinaTcpClient.class);
	private RemoteInfo remoteInfo;
	private SocketAddress rmtAddr;
	private SocketConnector connector;
	private IoSession session;
	private ActorId heartAid;
	private MinaMessageFactory messageFactory;
	
	private LinkedQueue<MinaRequestMessage> reqQue = 
		new LinkedQueue<MinaRequestMessage>(
			new SingleObjectPool<Node<MinaRequestMessage>>(
				new IObjectFactory() {
					@Override
					public IPooledObject createInstance(){
						return new Node<MinaRequestMessage>();
					}
				})
			);
	private final MinaIoFutureListener connectHandler;
	
//	OFFLINE<------+^----------+----->SENDING
//	   +          |           |         +
//	   |          |           +         |
//	   +------>CONNING+---->ONLINE<-----+

	public MinaTcpClient(
		Actor hostAx, 
		ConcurrentObjectPool<Node<Object>> cpool,
		ActorRelationMap relations,
//		ActorRelationMap monitorMap,
		RemoteInfo remoteInfo, 
		Pattern pattern
	){
		super(hostAx, cpool, relations);
		this.remoteInfo = remoteInfo;
		this.rmtAddr = new InetSocketAddress(remoteInfo.getHost(), remoteInfo.getPort());
		this.connectHandler = new MinaIoFutureListener(hostAx);
		pattern.match(
			MinaAtomCode.SOPENED,
			MinaAtomCode.SIDLE,
			MinaAtomCode.MRECVD,
			MinaAtomCode.SCLOSED,
			MinaAtomCode.EXCAUGHT,
			"HEART"
		);
	}

	@Override
	public void start() throws SuspendExecution, Exception {
		connector = new PooledNioSocketConnector(cpool);
		connector.setSessionDataStructureFactory(new PooledIoSessionDataStructureFactory(cpool));
		messageFactory = 
			new MinaMessageFactory(
//				hostAx.getActorSystem().getCustomMessageMap(), 
				hostAx.getActorSystem().getCMsgMap(),
				remoteInfo.getMinaMsgPoolSize(), 
				remoteInfo.getMinaMsgPoolInitSize(), 
				remoteInfo.getMinaMsgPoolMaxSize()
			);
		connector.getFilterChain().addLast("codec", new PooledProtocolCodecFilter(cpool, new MinaCodecFactory(messageFactory)));
		connector.setHandler(new MinaIoHandler(hostAx));
		connector.setConnectTimeoutMillis(TimeUnit.SECONDS.toMillis(remoteInfo.getMinaConnectTimeout()));
		
		SocketSessionConfig sscfg = connector.getSessionConfig();
		sscfg.setReadBufferSize(2048);
		sscfg.setMinReadBufferSize(2048);
		sscfg.setMaxReadBufferSize(2048);
		sscfg.setIdleTime(IdleStatus.READER_IDLE, remoteInfo.getMinaIdleTime());
		sscfg.setReceiveBufferSize(64 * 1024);
		sscfg.setSendBufferSize(160 * 1024);
		sscfg.setWriteTimeout(2);
		sscfg.setTcpNoDelay(true);
		sscfg.setSoLinger(-1);
		sscfg.setKeepAlive(false);
		
		heartAid = hostAx.getActorSystem().spawn(hostAx, hostAx.getFiberScheduler(), new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, InterruptedException {
				// 心跳计时器Actor
				Message cmsg = self.recv("INIT");
				ActorId sireAid = cmsg.getSender();
				
				Pattern pattern = new Pattern();
				pattern.match("STOP");
				pattern.after(remoteInfo.getHeartTimeout(), TimeUnit.SECONDS);
				
				while (true){
					try (PoolGuard guard = self.precv(pattern)){
						Message msg = guard.get();
						if (msg != null){
							String type = msg.getType();
							if (AtomCode.equals(type, "STOP")){
								break;
							}
						}else{
							self.send(sireAid, "HEART");
						}
					}catch (Throwable e){
						logger.error("{} heart error: {}", getClass().getSimpleName(), ExceptionUtils.printStackTrace(e));
					}
				}
			}
		}, LinkType.MONITORED);
		hostAx.send(heartAid, "INIT");
	}

	@Override
	public void handleMessage(Message msg) throws SuspendExecution, Exception {
		String type = msg.getType();
		switch (type){
		case MinaAtomCode.SOPENED:{
			if (session != null){
				session.closeNow();
			}
			session = msg.getRaw();
		}break;
		case MinaAtomCode.SIDLE:{
			IoSession s = msg.getRaw();
			s.closeNow();
			if (s == session){
				session = null;
				handleNetworkError(new RuntimeException("Network timeout"));
			}
		}break;
		case MinaAtomCode.MRECVD:{
			IoSession s = msg.getRaw();
			if (s == session){
				// 处理消息
				AbstractMinaMessage netMsg = msg.getRaw();
				if (netMsg instanceof MinaRequestMessage){
					handleRequest((MinaRequestMessage) netMsg);
				}else if (netMsg instanceof MinaPushMessage){
					handlePush((MinaPushMessage) netMsg);
				}else if (netMsg instanceof MinaResponseMessage){
					handleResponse((MinaResponseMessage) netMsg);
				}
			}
		}break;
		case MinaAtomCode.SCLOSED:{
			IoSession s = msg.getRaw();
			if (s == session){
				session = null;
				handleNetworkError(new RuntimeException("Network closed"));
			}
		}break;
		case MinaAtomCode.EXCAUGHT:{
			IoSession s = msg.getRaw();
			s.closeNow();
			if (s == session){
				session = null;
				Throwable e = msg.getRaw();
				handleNetworkError(e);
				if (logger.isErrorEnabled()){
					logger.error("{} exception: {}", getClass().getSimpleName(), ExceptionUtils.printStackTrace(e));
				}
			}
		}break;
		case "HEART":{
			Message smsg = Message.make();
			smsg.setSender(hostAx.getActorId());
			smsg.setType("HEART");
			sendRemote(ActorId.NULLAID, smsg);
		}break;
		}
	}

	@Override
	public void handleException(Throwable e) throws SuspendExecution {
		logger.error("{} error: {}", getClass().getSimpleName(), ExceptionUtils.printStackTrace(e));
	}

	@Override
	public void stop() throws SuspendExecution {
		if (session != null){
			session.closeNow();
		}
		handleNetworkError(new RuntimeException("Network closed by user"));
		connector.dispose();
		if (heartAid != null){
			hostAx.send(heartAid, "STOP");
			try {
				hostAx.recvExit(heartAid);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void sendRemote(ActorId toAid, Message smsg) throws SuspendExecution, Exception {
		if (session == null){
			// 尝试连接
//			System.out.println(getClass().getSimpleName()+" start connect");
			connector.connect(rmtAddr).addListener(connectHandler);
			Message cmsg = hostAx.recv(MinaAtomCode.SOPENED, "CONNERR");
			switch (cmsg.getType()){
			case MinaAtomCode.SOPENED:{
				session = cmsg.getRaw();
				if (!AtomCode.equals("HEART", smsg.getType())){
					// 发送一次心跳
					Message msg = Message.make();
					msg.setSender(hostAx.getActorId());
					msg.setType("HEART");
					MinaRequestMessage req = messageFactory.createInstance(MinaMsgCode.HEART_REQ);
					reqQue.add(req);
					req.setToAid(ActorId.NULLAID);
					req.setMessage(msg);
					session.write(req);
//					System.out.println(getClass().getSimpleName()+" connected, send heart");
				}
			}break;
			case "CONNERR":{
				smsg.release();
				Throwable e = cmsg.getRaw();
				handleNetworkError(e);
				return;
			}}
		}
		
		boolean recycleMsg = false;
		AbstractMinaWrapMessage wrap = null;
		switch(smsg.getType()){
		case "HEART":{
			MinaRequestMessage req = messageFactory.createInstance(MinaMsgCode.HEART_REQ);
			reqQue.add(req);
			wrap = req;
		}break;
		case AtomCode.LINK:{
			MinaRequestMessage req = messageFactory.createInstance(MinaMsgCode.LINK_REQ);
			reqQue.add(req);
			wrap = req;
		}break;
		case AtomCode.MONITOR:{
			MinaRequestMessage req = messageFactory.createInstance(MinaMsgCode.MONITOR_REQ);
			reqQue.add(req);
			wrap = req;
		}break;
		case AtomCode.EXIT:{
			MinaPushMessage push = messageFactory.createInstance(MinaMsgCode.EXIT_PUSH);
			wrap = push;
			recycleMsg = true;
			actorExit(smsg.getSender(), toAid);
		}break;
		default:{
			MinaPushMessage push = messageFactory.createInstance(MinaMsgCode.SEND_PUSH);
			wrap = push;
			recycleMsg = true;
		}break;
		}
		wrap.setToAid(toAid);
		wrap.setMessage(smsg);
		session.write(wrap);
		if (recycleMsg){
			wrap.release();
			smsg.release();
		}
	}
	
	private void handleRequest(MinaRequestMessage req) throws SuspendExecution {
		MinaResponseMessage resp = null;
		Message rmsg = null;
		try{
			MinaMsgCode msgCode = req.getMsgCode();
			switch (msgCode){
			case LINK_REQ:
			case MONITOR_REQ:{
				ActorId toAid = req.getToAid();
				rmsg = req.getMessage();
				ActorId remoteAid = rmsg.getSender();
				
				ActorExit axExit = Actor.linkFromRemote(axSys, remoteAid, toAid);
				MinaMsgCode respCode = msgCode == MinaMsgCode.LINK_REQ ? MinaMsgCode.LINK_RESP : MinaMsgCode.MONITOR_RESP;
				resp = messageFactory.createInstance(respCode);
				if (axExit != null){
					resp.setErrcode(ErrorCode.FAILURE);
					((MinaRelationResponse) resp).setActorExit(axExit);
				}else{
					relations.addRelation(remoteAid, toAid);
				}
				session.write(resp);
			}break;
			default:
				throw new AssertionError(msgCode);
			}
		}finally{
			req.release();
			if (resp != null){
				resp.release();
			}
			if (rmsg != null){
				rmsg.release();
			}
		}
	}
	
	private void handlePush(MinaPushMessage push) throws SuspendExecution {
		try{
			ActorId toAid = push.getToAid();
			Message rmsg = push.getMessage();
			MinaMsgCode msgCode = push.getMsgCode();
			switch (msgCode){
			case EXIT_PUSH:{
				actorExit(rmsg.getSender(), toAid);
				axSys.send(toAid, rmsg);
			}break;
			case SEND_PUSH:{
				axSys.send(toAid, rmsg);
			}break;
			default:
				throw new AssertionError(msgCode);
			}
		}finally{
			push.release();
		}
	}
	
	private void handleResponse(MinaResponseMessage resp){
		MinaRequestMessage req = null;
		Message wmsg = null;
		try{
			req = reqQue.poll();
			ActorId toAid = req.getToAid();
			wmsg = req.getMessage();
			wmsg.resetRead();
			String type = wmsg.getType();
			ActorId sender = wmsg.getSender();
			MinaMsgCode msgCode = resp.getMsgCode();
			switch (msgCode){
			case HEART_RESP:{
				if (!AtomCode.equals(type, "HEART")){
					throw new AssertionError(type);
				}
			}break;
			case LINK_RESP:{
				if (!AtomCode.equals(type, AtomCode.LINK)){
					throw new AssertionError(type);
				}
			}
			case MONITOR_RESP:{
				if (!AtomCode.equals(type, AtomCode.MONITOR)){
					throw new AssertionError(type);
				}
//				ActorId toAid = req.get(new ActorId());
//				ActorId sender = wmsg.getSender();
				
				ErrorCode errc = resp.getErrcode();
				if (errc != ErrorCode.SUCCESS){
					MinaRelationResponse relationResp = (MinaRelationResponse) resp;
					ActorExit axExit = relationResp.getActorExit();
					axSys.send(toAid, sender, AtomCode.EXIT, axExit);
				}else{
					relations.addRelation(sender, toAid);
					axSys.send(toAid, sender, type);
				}
			}break;
			default:
				throw new AssertionError(msgCode);
			}
		}finally{
			resp.release();
			req.release();
			wmsg.release();
		}
	}
	
	private void handleNetworkError(Throwable e) throws SuspendExecution {
		String errmsg = ExceptionUtils.printStackTrace(e);
		super.sendNetErrorExits(errmsg);
		
		MinaRequestMessage req = null;
		Message wmsg = null;
		while ((req = reqQue.poll()) != null){
			try{
				ActorId toAid = req.getToAid();
				wmsg = req.getMessage();
				wmsg.resetRead();
				String type = wmsg.getType();
				switch (type){
				case "HEART":{
				}break;
				case AtomCode.LINK:
				case AtomCode.MONITOR:{
					super.sendNetErrorExit(toAid, wmsg.getSender(), errmsg);
				}break;
				default:{
				}break;
				}
			}finally{
				req.release();
				wmsg.release();
			}
		}
	}

}
