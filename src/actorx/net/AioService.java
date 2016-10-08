/**
 * 
 */
package actorx.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import actorx.Actor;
import actorx.ActorId;
import actorx.AxSystem;
import actorx.IRecvFilter;
import actorx.Message;
import actorx.MsgType;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:38:56
 *
 */
public class AioService implements IRecvFilter {

	public AioService(Actor hostAx) throws IOException{
		this.chanGroup = hostAx.getAxSystem().getChannelGroup();
		AxSystem axs = hostAx.getAxSystem();
		ActorId hostAid = hostAx.getActorId();
		this.readHandler = new ReadHandler();
		this.writeHandler = new WriteHandler();
		this.acceptHandler = new AcceptHandler(axs, hostAid, readHandler, writeHandler);
		this.connectHandler = new ConnectHandler(axs, hostAid, readHandler, writeHandler);
		hostAx.addRecvFilter(PEER_CLOSE, this);
		hostAx.addRecvFilter(READ_ERR, this);
		hostAx.addRecvFilter(WRITE_END, this);
		hostAx.addRecvFilter(WRITE_ERR, this);
	}
	
	public void connect(InetSocketAddress inetSocketAddress, AbstractAioSession as) throws IOException{
		connect(new ConnectOptions().inetSocketAddress(inetSocketAddress), as);
	}
	
	public void connect(ConnectOptions opt, AbstractAioSession as) throws IOException{
		AsynchronousSocketChannel skt = AsynchronousSocketChannel.open(chanGroup);
		skt.setOption(StandardSocketOptions.SO_REUSEADDR, opt.socketOptions.soReuseAddr);
		skt.setOption(StandardSocketOptions.SO_RCVBUF, opt.socketOptions.soRcvBuf);
		skt.setOption(StandardSocketOptions.SO_SNDBUF, opt.socketOptions.soSndBuf);
//		skt.setOption(StandardSocketOptions.SO_LINGER, opt.socketOptions.soLinger);
		skt.setOption(StandardSocketOptions.TCP_NODELAY, opt.socketOptions.tcpNodelay);
		as.setSocket(skt);
		skt.connect(opt.inetSocketAddress, as, connectHandler);
	}
	
	public InetSocketAddress listen(AbstractListenSession ls) throws IOException{
		return listen(new ListenOptions(), ls);
	}
	
	public InetSocketAddress listen(InetSocketAddress inetSocketAddress, AbstractListenSession ls) throws IOException{
		return listen(new ListenOptions().inetSocketAddress(inetSocketAddress), ls);
	}
	
	public InetSocketAddress listen(ListenOptions opt, AbstractListenSession ls) throws IOException{
		AsynchronousServerSocketChannel acpr = AsynchronousServerSocketChannel.open(chanGroup);
		acpr.setOption(StandardSocketOptions.SO_REUSEADDR, opt.socketOptions.soReuseAddr);
		acpr.setOption(StandardSocketOptions.SO_RCVBUF, opt.socketOptions.soRcvBuf);
		
		// 绑定
		acpr.bind(opt.inetSocketAddress, opt.backlog);
		
		InetSocketAddress inetSocketAddress = (InetSocketAddress) acpr.getLocalAddress();
		ls.setAcceptor(acpr);
		acpr.accept(ls, acceptHandler);
		return inetSocketAddress;
	}
	
	public Message filterRecv(ActorId fromAid, String type, Message prevMsg, Message srcMsg){
		AbstractAioSession as = prevMsg.getRaw();
		as.onFilterRecv(type);
		return null;
	}
	
	static class ReadHandler implements CompletionHandler<Integer, AbstractAioSession> {

		@Override
		public void completed(Integer result, AbstractAioSession attachment) {
			attachment.onRead(result);
		}

		@Override
		public void failed(Throwable exc, AbstractAioSession attachment) {
			attachment.onReadFailed(exc);
		}
		
	}
	
	static class WriteHandler implements CompletionHandler<Integer, AbstractAioSession> {

		@Override
		public void completed(Integer result, AbstractAioSession attachment) {
			attachment.onWrite(result);
		}

		@Override
		public void failed(Throwable exc, AbstractAioSession attachment) {
			attachment.onWriteFailed(exc);
		}
		
	}
	
	static class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AbstractListenSession> {
		private AxSystem axs;
		private ActorId hostAid;
		private ReadHandler readHandler;
		private WriteHandler writeHandler;
		
		public AcceptHandler(AxSystem axs, ActorId hostAid, ReadHandler readHandler, WriteHandler writeHandler){
			this.axs = axs;
			this.hostAid = hostAid;
			this.readHandler = readHandler;
			this.writeHandler = writeHandler;
		}

		@Override
		public void completed(AsynchronousSocketChannel result, AbstractListenSession attachment) {
			attachment.getAcceptor().accept(attachment, this);
			AbstractAioSession as = attachment.makeAioSession(result);
			as.onOpen(axs, hostAid, readHandler, writeHandler);
			axs.send(null, hostAid, MsgType.OPEN, attachment, as);
		}

		@Override
		public void failed(Throwable exc, AbstractListenSession attachment) {
			attachment.close();
			axs.send(null, hostAid, MsgType.ACCEPT_ERR, exc, attachment);
		}
		
	}
	
	static class ConnectHandler implements CompletionHandler<Void, AbstractAioSession> {
		private AxSystem axs;
		private ActorId hostAid;
		private ReadHandler readHandler;
		private WriteHandler writeHandler;
		
		public ConnectHandler(AxSystem axs, ActorId hostAid, ReadHandler readHandler, WriteHandler writeHandler){
			this.axs = axs;
			this.hostAid = hostAid;
			this.readHandler = readHandler;
			this.writeHandler = writeHandler;
		}

		@Override
		public void completed(Void result, AbstractAioSession attachment) {
			attachment.onOpen(axs, hostAid, readHandler, writeHandler);
			axs.send(null, hostAid, MsgType.OPEN, attachment);
		}

		@Override
		public void failed(Throwable exc, AbstractAioSession attachment) {
			attachment.closesocket();
			axs.send(null, hostAid, MsgType.CONN_ERR, exc, attachment);
		}
		
	}

	private AsynchronousChannelGroup chanGroup;
	private ReadHandler readHandler;
	private WriteHandler writeHandler;
	private AcceptHandler acceptHandler;
	private ConnectHandler connectHandler;
	
	// 内部消息类型
	static final String PEER_CLOSE = "PEER_CLOSE";
	static final String READ_ERR = "READ_ERR";
	static final String WRITE_END = "WRITE_END";
	static final String WRITE_ERR = "WRITE_ERR";
}
