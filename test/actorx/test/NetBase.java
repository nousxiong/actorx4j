/**
 * 
 */
package actorx.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.AxSystem;
import actorx.ExitType;
import actorx.IActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageGuard;
import actorx.MsgType;
import actorx.Packet;
import actorx.Pattern;
import actorx.net.AbstractAioSession;
import actorx.net.AbstractListenSession;
import actorx.net.AioService;

/**
 * @author Xiong
 * @creation 2016年10月6日下午2:03:24
 *
 */
public class NetBase {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	static enum MsgCode {
		ECHO((short) 1),
		;
		
		private short code;
		public short getCode(){
			return code;
		}
		
		public static MsgCode parse(short code){
			for (MsgCode msgCode : values()){
				if (code == msgCode.getCode()){
					return msgCode;
				}
			}
			throw new RuntimeException("MsgCode enum parse error, code: "+code);
		}
		
		private MsgCode(short code){
			this.code = code;
		}
	}
	
	/**
	 * 用户自定义消息
	 * @author Xiong
	 * @creation 2016年10月1日下午2:01:36
	 *
	 */
	static interface IMessage {
		public static final int HEAD_LENGTH = 4;
		public static final int DEFAULT_BODY_LENGTH = 512;
		
		public MsgCode getMsgCode();
		public void decode(ByteBuffer buffer);
		public ByteBuffer encode();
	}
	
	static abstract class AbstractMessage implements IMessage {
		private MsgCode msgCode;
		
		public AbstractMessage(MsgCode msgCode){
			this.msgCode = msgCode;
		}
		
		@Override
		public MsgCode getMsgCode(){
			return msgCode;
		}
		
		@Override
		public final void decode(ByteBuffer buffer){
			decodeBody(buffer);
		}
		
		public abstract void decodeBody(ByteBuffer buffer);
		
		@Override
		public final ByteBuffer encode(){
			short length = getBodyLengthHint();
			ByteBuffer buffer = ByteBuffer.allocate(IMessage.HEAD_LENGTH + length);
			buffer.putShort((short) 0);
			buffer.putShort(msgCode.getCode());
			encodeBody(buffer);
			buffer.putShort(0, (short) (buffer.position() - IMessage.HEAD_LENGTH));
			buffer.flip();
			return buffer;
		}
		
		public short getBodyLengthHint(){
			return IMessage.DEFAULT_BODY_LENGTH;
		}
		
		public abstract void encodeBody(ByteBuffer buffer);
	}
	
	static class EchoMessage extends AbstractMessage {
		private String echo = "";

		public EchoMessage() {
			super(MsgCode.ECHO);
		}

		@Override
		public void decodeBody(ByteBuffer buffer) {
			short length = buffer.getShort();
			byte[] echoBytes = new byte[length];
			buffer.get(echoBytes);
			echo = new String(echoBytes);
		}

		@Override
		public void encodeBody(ByteBuffer buffer) {
			byte[] echoBytes = echo.getBytes();
			buffer.putShort((short) echoBytes.length);
			buffer.put(echoBytes);
		}
		
		@Override
		public short getBodyLengthHint(){
			return (short) (echo.length() + 2);
		}

		public String getEcho() {
			return echo;
		}

		public void setEcho(String echo) {
			this.echo = echo;
		}
		
	}
	
	static class AioSession extends AbstractAioSession {

		@Override
		protected void doDecode(ByteBuffer buffer) {
			if (buffer.remaining() < IMessage.HEAD_LENGTH){
				return;
			}
			
			// 标记当前位置
			buffer.mark();
			short length = buffer.getShort();
			short code = buffer.getShort();
			
			if (buffer.remaining() < length){
				// 内容不足时重置并返回
				buffer.reset();
				return;
			}
			
			MsgCode msgCode = MsgCode.parse(code);
			IMessage msg = null;
			if (msgCode == MsgCode.ECHO){
				msg = new EchoMessage();
			}else{
				throw new RuntimeException("MsgCode not found: "+msgCode);
			}
			
			msg.decode(buffer);
			sendReadResult(msg);
		}
		
		public void sendEcho(String echo){
			EchoMessage echoMsg = new EchoMessage();
			echoMsg.setEcho(echo);
			write(echoMsg.encode());
		}

	}
	
	static class ListenSession extends AbstractListenSession {
		private int acceptNum = 0;
		
		public ListenSession(int acceptNum){
			this.acceptNum = acceptNum;
		}

		@Override
		public AbstractAioSession makeAioSession(AsynchronousSocketChannel skt) {
			AioSession as = new AioSession();
			as.setSocket(skt);
			return as;
		}
		
		public boolean updateAcceptNum(){
			if (--acceptNum == 0){
				return true;
			}else{
				return false;
			}
		}
	}
	
	static class AioClient implements IActorHandler {

		@Override
		public void run(Actor self) throws Exception {
			Packet pkt = self.recvPacket("INIT");
			int connNum = pkt.getInt();
			int addrNum = pkt.getInt();
			int startPort = pkt.getInt();
			int totalNum = connNum * addrNum;
			
			AioService aioSvc = new AioService(self);
			for (int i=0; i<connNum; ++i){
				for (int n=0; n<addrNum; ++n){
					aioSvc.connect(new InetSocketAddress("127.0.0.1", startPort + n), new AioSession());
				}
			}
			
			int closed = 0;
			Pattern pattern = new Pattern();
			pattern.match(MsgType.OPEN, MsgType.CONN_ERR, MsgType.RECV, MsgType.EXCEPT, MsgType.CLOSE);
			
			boolean goon = true;
			while (goon){
				try (MessageGuard guard = self.recv(pattern)){
					Message msg = guard.get();
					String type = msg.getType();
					switch (type){
						case MsgType.OPEN: {
							AioSession as = msg.getRaw();
							as.sendEcho("Hi, my name is AioClient!");
						}break;
						case MsgType.CONN_ERR: {
							System.out.println("connect error");
							Throwable exc = msg.getRaw();
							throw exc;
						}
						case MsgType.RECV: {
							AioSession as = msg.getRaw();
							as.close();
							++closed;
						}break;
						case MsgType.CLOSE: {
							if (--totalNum == 0){
								System.out.println("AioClient quiting ...");
								goon = false;
							}
						}break;
						case MsgType.EXCEPT: {
							Throwable exc = msg.getRaw();
							throw exc;
						}
					}
				}catch (AsynchronousCloseException e){
					
				}catch (Throwable e){
					e.printStackTrace();
				}
			}
			System.out.println("AioClient exited, closed: "+closed);
		}
		
	}
	
	static class AioServer implements IActorHandler {

		@Override
		public void run(Actor self) throws Exception {
			Packet pkt = self.recvPacket("INIT");
			ActorId sireAid = pkt.getSender();
			int acceptNum = pkt.getInt();
			int addrNum = pkt.getInt();
			int startPort = pkt.getInt();
			int totalNum = acceptNum * addrNum;

			AioService aioSvc = new AioService(self);
			for (int i=0; i<addrNum; ++i){
				aioSvc.listen(new InetSocketAddress(startPort + i), new ListenSession(acceptNum));
			}

			int acceptOk = 0;
			Pattern pattern = new Pattern();
			pattern.match(MsgType.OPEN, MsgType.ACCEPT_ERR, MsgType.RECV, MsgType.EXCEPT, MsgType.CLOSE);
			
			// 发送准备完毕消息
			self.send(sireAid, "READY");
			
			List<AbstractListenSession> lss = new ArrayList<AbstractListenSession>(addrNum);
			while (true){
				try (MessageGuard guard = self.recv(pattern)){
					Message msg = guard.get();
					String type = msg.getType();
					switch (type){
						case MsgType.OPEN: {
							AbstractListenSession ls = msg.getRaw();
							lss.add(ls);
							++acceptOk;
						}break;
						case MsgType.ACCEPT_ERR: {
							Throwable exc = msg.getRaw();
							throw exc;
						}
						case MsgType.RECV: {
							AioSession as = msg.getRaw();
							EchoMessage echoMsg = msg.getRaw();
							as.sendEcho(echoMsg.getEcho());
						}break;
						case MsgType.CLOSE: {
							if (--totalNum == 0){
								System.out.println("AioServer quiting ...");
								for (AbstractListenSession ls : lss){
									ls.close();
								}
							}
						}break;
						case MsgType.EXCEPT: {
							Throwable exc = msg.getRaw();
							throw exc;
						}
					}
				}catch (AsynchronousCloseException e){
					if (--addrNum == 0){
						break;
					}
				}catch (Throwable e){
				}
			}
			System.out.println("AioServer exited, acceptOk: "+acceptOk);
		}
		
	}

	@Test
	public void test() throws IOException {
		AxSystem axs = new AxSystem("AXS");
		axs.startup();
		
		int connNum = 3;
		int addrNum = 3;
		int startPort = 23333;
		Actor baseAx = axs.spawn();
		ActorId svrAid = axs.spawn(baseAx, new AioServer(), LinkType.MONITORED);
		baseAx.send(svrAid, "INIT", connNum, addrNum, startPort);
		baseAx.recvPacket("READY");
		
		ActorId clnAid = axs.spawn(baseAx, new AioClient(), LinkType.MONITORED);
		baseAx.send(clnAid, "INIT", connNum, addrNum, startPort);
		
		for (int i=0; i<2; ++i){
			ActorExit aex = baseAx.recvExit();
			if (aex.getExitType() == ExitType.EXCEPT){
				System.out.println("Actor "+aex.getSender()+" quit except, errmsg: "+aex.getErrmsg());
			}
		}
		
		axs.shutdown();
		System.out.println("done.");
	}

}
