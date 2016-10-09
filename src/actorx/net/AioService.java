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
import actorx.Addon;
import actorx.IRecvFilter;
import actorx.Message;
import actorx.MsgType;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:38:56
 *
 */
public class AioService extends Addon implements IRecvFilter {

	public AioService(Actor hostAx) throws IOException{
		super(hostAx);
		this.chanGroup = super.getAxSystem().getChannelGroup();
		this.readHandler = new ReadHandler();
		this.writeHandler = new WriteHandler();
		this.acceptHandler = new AcceptHandler(this, readHandler, writeHandler);
		this.connectHandler = new ConnectHandler(this, readHandler, writeHandler);
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
	
	@Override
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
		private Addon addon;
		private ReadHandler readHandler;
		private WriteHandler writeHandler;
		
		public AcceptHandler(Addon addon, ReadHandler readHandler, WriteHandler writeHandler){
			this.addon = addon;
			this.readHandler = readHandler;
			this.writeHandler = writeHandler;
		}

		@Override
		public void completed(AsynchronousSocketChannel result, AbstractListenSession attachment) {
			attachment.getAcceptor().accept(attachment, this);
			AbstractAioSession as = attachment.makeAioSession(result);
			as.onOpen(addon, readHandler, writeHandler);
			addon.send(MsgType.OPEN, attachment, as);
		}

		@Override
		public void failed(Throwable exc, AbstractListenSession attachment) {
			attachment.close();
			addon.send(MsgType.ACCEPT_ERR, exc, attachment);
		}
		
	}
	
	static class ConnectHandler implements CompletionHandler<Void, AbstractAioSession> {
		private Addon addon;
		private ReadHandler readHandler;
		private WriteHandler writeHandler;
		
		public ConnectHandler(Addon addon, ReadHandler readHandler, WriteHandler writeHandler){
			this.addon = addon;
			this.readHandler = readHandler;
			this.writeHandler = writeHandler;
		}

		@Override
		public void completed(Void result, AbstractAioSession attachment) {
			attachment.onOpen(addon, readHandler, writeHandler);
			addon.send(MsgType.OPEN, attachment);
		}

		@Override
		public void failed(Throwable exc, AbstractAioSession attachment) {
			attachment.closesocket();
			addon.send(MsgType.CONN_ERR, exc, attachment);
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
