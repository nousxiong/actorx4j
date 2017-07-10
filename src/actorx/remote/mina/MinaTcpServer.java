/**
 * 
 */
package actorx.remote.mina;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.SuspendExecution;
import cque.ConcurrentObjectPool;
import cque.IObjectFactory;
import cque.IPooledObject;
import cque.LinkedQueue;
import cque.Node;
import cque.SingleObjectPool;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.AtomCode;
import actorx.Message;
import actorx.Pattern;
import actorx.addon.mina.MinaAtomCode;
import actorx.addon.mina.MinaIoHandler;
import actorx.remote.ActorRelationMap;
import actorx.remote.ErrorCode;
import actorx.remote.INetworkServerService;
import actorx.remote.LocalInfo;
import actorx.remote.NetSession;
import actorx.remote.mina.io.AbstractMinaMessage;
import actorx.remote.mina.io.AbstractMinaWrapMessage;
import actorx.remote.mina.io.MinaCodecFactory;
import actorx.remote.mina.io.MinaMessageFactory;
import actorx.remote.mina.io.MinaMsgCode;
import actorx.remote.mina.io.push.MinaPushMessage;
import actorx.remote.mina.io.request.MinaRequestMessage;
import actorx.remote.mina.io.response.MinaRelationResponse;
import actorx.remote.mina.io.response.MinaResponseMessage;
import actorx.util.ExceptionUtils;
import amina.codec.PooledProtocolCodecFilter;
import amina.session.PooledIoSessionDataStructureFactory;
import amina.transport.socket.nio.PooledNioSocketAcceptor;

/**
 * @author Xiong
 *
 */
public class MinaTcpServer extends AbstractMinaService implements INetworkServerService {
	private static final Logger logger = LoggerFactory.getLogger(MinaTcpServer.class);
	private LocalInfo localInfo;
	private SocketAcceptor acceptor;
	private MinaMessageFactory messageFactory;
	private static final int SESSION_CLOSED_ATTR = 1;
	private static final int AXID_ATTR = 2;
	private static final int MINA_SESSION = 3;
	
	private SingleObjectPool<Node<MinaRequestMessage>> requsetPool = 
		new SingleObjectPool<Node<MinaRequestMessage>>(
			new IObjectFactory() {
				@Override
				public IPooledObject createInstance(){
					return new Node<MinaRequestMessage>();
				}
			});

	public MinaTcpServer(
		Actor hostAx, 
		ConcurrentObjectPool<Node<Object>> cpool,
		ActorRelationMap relations, 
		LocalInfo localInfo, 
		Pattern pattern
	){
		super(hostAx, cpool, relations);
		this.localInfo = localInfo;
		pattern.match(
			MinaAtomCode.SOPENED,
			MinaAtomCode.SIDLE,
			MinaAtomCode.MRECVD,
			MinaAtomCode.SCLOSED,
			MinaAtomCode.EXCAUGHT
		);
	}

	@Override
	public void start() throws SuspendExecution, Exception {
		// Acceptor
		acceptor = new PooledNioSocketAcceptor(cpool);
		acceptor.setBacklog(1024);
		acceptor.setSessionDataStructureFactory(new PooledIoSessionDataStructureFactory(cpool));
		messageFactory = 
			new MinaMessageFactory(
//				hostAx.getActorSystem().getCustomMessageMap(), 
				hostAx.getActorSystem().getCMsgMap(),
				localInfo.getMinaMsgPoolSize(), 
				localInfo.getMinaMsgPoolInitSize(), 
				localInfo.getMinaMsgPoolMaxSize()
			);
		acceptor.getFilterChain().addLast("codec", new PooledProtocolCodecFilter(cpool, new MinaCodecFactory(messageFactory)));
		
		// Handler
		acceptor.setHandler(new MinaIoHandler(hostAx));
		
		// Config
		acceptor.setReuseAddress(true);
		SocketSessionConfig sscfg = acceptor.getSessionConfig();
		sscfg.setReadBufferSize(2048);
		sscfg.setMinReadBufferSize(2048);
		sscfg.setMaxReadBufferSize(2048);
		sscfg.setIdleTime(IdleStatus.READER_IDLE, localInfo.getMinaIdleTime());
		sscfg.setReceiveBufferSize(64 * 1024);
		sscfg.setSendBufferSize(160 * 1024);
		sscfg.setWriteTimeout(2);
		sscfg.setTcpNoDelay(true);
		sscfg.setSoLinger(-1);
		sscfg.setKeepAlive(false);
		
		// Bind
		List<SocketAddress> socketAddresses = new ArrayList<SocketAddress>();
		socketAddresses.add(new InetSocketAddress(localInfo.getHost(), localInfo.getPort()));
		acceptor.bind(socketAddresses);
	}

	@Override
	public void handleMessage(Message msg) throws SuspendExecution, Exception {
		String type = msg.getType();
		switch (type){
		case MinaAtomCode.SOPENED:{
			IoSession s = msg.getRaw();
			s.setAttribute(SESSION_CLOSED_ATTR, false);
			s.setAttribute(AXID_ATTR, (long) -1);
		}break;
		case MinaAtomCode.SIDLE:{
			IoSession s = msg.getRaw();
			s.closeNow();
			s.setAttribute(SESSION_CLOSED_ATTR, true);
			handleNetworkError(s, new RuntimeException("Network timeout"));
		}break;
		case MinaAtomCode.MRECVD:{
			IoSession s = msg.getRaw();
			AbstractMinaMessage netMsg = msg.getRaw();
			if (netMsg instanceof MinaRequestMessage){
				handleRequest(s, (MinaRequestMessage) netMsg);
			}else if (netMsg instanceof MinaPushMessage){
				handlePush(s, (MinaPushMessage) netMsg);
			}else if (netMsg instanceof MinaResponseMessage){
				handleResponse(s, (MinaResponseMessage) netMsg);
			}
		}break;
		case MinaAtomCode.SCLOSED:{
			IoSession s = msg.getRaw();
			long axid = (long) s.getAttribute(AXID_ATTR);
			axSys.getNetworkManager().removeNetSession(axid);
			boolean alreadyClosed = (boolean) s.getAttribute(SESSION_CLOSED_ATTR);
			if (!alreadyClosed){
				s.setAttribute(SESSION_CLOSED_ATTR, true);
				handleNetworkError(s, new RuntimeException("Network closed"));
			}
			s.removeAttribute(MINA_SESSION);
		}break;
		case MinaAtomCode.EXCAUGHT:{
			IoSession s = msg.getRaw();
			s.closeNow();
			s.setAttribute(SESSION_CLOSED_ATTR, true);
			Throwable e = msg.getRaw();
			handleNetworkError(s, e);
			if (logger.isErrorEnabled()){
				logger.error("{} exception: {}", getClass().getSimpleName(), ExceptionUtils.printStackTrace(e));
			}
		}break;
		}
	}

	@Override
	public void handleException(Throwable e) throws SuspendExecution {
		logger.error("{} error: {}", getClass().getSimpleName(), ExceptionUtils.printStackTrace(e));
	}

	@Override
	public void stop() {
		acceptor.unbind();
		acceptor.dispose();
	}

	@Override
	public void sendRemote(ActorId toAid, Message smsg, NetSession netSession) throws SuspendExecution, Exception {
		MinaSession minaSession = (MinaSession) netSession;
		LinkedQueue<MinaRequestMessage> reqQue = minaSession.getRequestQueue();
		IoSession session = minaSession.getSession();
		
		boolean recycleMsg = false;
		AbstractMinaWrapMessage wrap = null;
		switch(smsg.getType()){
		case "HEART":{
			throw new AssertionError();
		}
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
	
	private void handleRequest(IoSession session, MinaRequestMessage req) throws SuspendExecution {
		MinaResponseMessage resp = null;
		Message rmsg = null;
		try{
			MinaMsgCode msgCode = req.getMsgCode();
			switch (msgCode){
			case HEART_REQ:{
				rmsg = req.getMessage();
				long axid = rmsg.getSender().axid;
				if (!axSys.getNetworkManager().hasNetSession(axid)){
					MinaSession minaSession = new MinaSession(requsetPool);
					minaSession.setSession(session);
					minaSession.setHandleAid(hostAx.getActorId());
					if (axSys.getNetworkManager().addNetSession(axid, minaSession) != null){
						session.setAttribute(AXID_ATTR, axid);
						session.setAttribute(MINA_SESSION, minaSession);
					}
				}
				resp = messageFactory.createInstance(MinaMsgCode.HEART_RESP);
				session.write(resp);
			}break;
			case LINK_REQ:
			case MONITOR_REQ:{
				ActorId toAid = req.getToAid();
				rmsg = req.getMessage();
				ActorId remoteAid = rmsg.getSender();
				
				ActorExit axExit = Actor.linkFromRemote(axSys, remoteAid, toAid);
				MinaMsgCode respCode = msgCode == MinaMsgCode.LINK_REQ ? MinaMsgCode.LINK_RESP : MinaMsgCode.MONITOR_RESP;
				resp = messageFactory.createInstance(respCode);
				if (axExit != null){
//					System.out.println("MinaTcpServer link/monitor error: "+axExit.getExitType());
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
	
	private void handlePush(IoSession session, MinaPushMessage push) throws SuspendExecution {
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
	
	private void handleResponse(IoSession session, MinaResponseMessage resp) throws SuspendExecution {
		MinaSession minaSession = (MinaSession) session.getAttribute(MINA_SESSION);
		LinkedQueue<MinaRequestMessage> reqQue = minaSession.getRequestQueue();
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
				throw new AssertionError(type);
			}
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
	
	private void handleNetworkError(IoSession session, Throwable e) throws SuspendExecution {
		String errmsg = ExceptionUtils.printStackTrace(e);
		super.sendNetErrorExits(errmsg);
	}

}
