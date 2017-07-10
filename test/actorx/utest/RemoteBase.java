/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.BeforeClass;
import org.junit.Test;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import cque.util.PoolGuard;
import actorx.AbstractCustomMessage;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ActorSystemConfig;
import actorx.AtomCode;
import actorx.ExitType;
import actorx.ICustomMessageFactory;
import actorx.IThreadActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.remote.LocalInfo;
import actorx.remote.ProtocolType;
import actorx.remote.RemoteInfo;
import actorx.util.StringUtils;
import adata.Stream;
import amina.core.buffer.PooledBufferAllocator;

/**
 * @author Xiong
 *
 */
public class RemoteBase {
	static class Hello extends AbstractCustomMessage {
		private String tag = "";
		public static final String MTYPE = "HELLO";
		public static final String CTYPE = "rmt.Hello";
		
		public Hello(){
			this("");
		}
		
		public Hello(String tag){
			super(CTYPE);
			super.setType(MTYPE);
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		}

		@Override
		public void cread(Stream stream) {
			tag = stream.readString();
		}

		@Override
		public void cwrite(Stream stream) {
			stream.writeString(tag);
		}

		@Override
		public int csizeOf() {
			return Stream.sizeOfString(tag);
		}
	}
	
	static class HelloFactory implements ICustomMessageFactory {
		@Override
		public AbstractCustomMessage createInstance(){
			return new Hello();
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PropertyConfigurator.configure("test/actorx/utest/log4j.properties");
		// 初始化Allocator
		IoBuffer.setUseDirectBuffer(false);
		IoBuffer.setAllocator(new PooledBufferAllocator(1000));
	}

	@Test
	public void test() throws SuspendExecution, InterruptedException {
		handleTest(-1);
		System.out.println("Remote test begin...");
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("Thread test all done, average eclipse: "+eclipse);
	}
	
	long handleTest(int idx) throws SuspendExecution, InterruptedException {
		final String SRV_ADDRESS = "tcp://127.0.0.1:23333";
//		final String CLN_ADDRESS = "tcp://127.0.0.1:23334";
		ActorSystem clnAxs = new ActorSystem(new ActorSystemConfig().setAxid("AXSCLN").addCustomMessage(Hello.CTYPE, new HelloFactory()));
		ActorSystem srvAxs = new ActorSystem(new ActorSystemConfig().setAxid("AXSSRV").addCustomMessage(Hello.CTYPE, new HelloFactory()));
		
		srvAxs.startup();
		clnAxs.startup();

		clnAxs.addRemote("AXSSRV", new RemoteInfo().parseRemote(ProtocolType.MINA_TCP, SRV_ADDRESS));
		srvAxs.listen(new LocalInfo().parseLocal(ProtocolType.MINA_TCP, SRV_ADDRESS));
		Strand.sleep(100);
		
		long bt = System.nanoTime();
		Actor clnBaseAx = clnAxs.spawn();
		Actor srvBaseAx = srvAxs.spawn();
		
		ActorId srvAid = srvAxs.spawn(srvBaseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Message cmsg = self.recv("INIT");
				ActorId baseAid = cmsg.getSender();
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HELLO");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					try (PoolGuard guard = self.precv(patt)){
						Message msg = guard.get();
						if (msg == null){
							// 超时
							continue;
						}
						
						if (msg instanceof Hello){
							Hello hello = (Hello) msg;
							String tag = hello.getTag();
							assertTrue(StringUtils.equals("Hello World!", tag));
							self.send(baseAid, "HI", tag);
						}else{
							String str = msg.getString();
							if (AtomCode.equals("end", str)){
								goon = false;
							}
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
				self.send(baseAid, "OK", "Bye!");
				
				// 测试异常退出
				throw new Exception("server thread test exception");
			}
		}, 
		LinkType.MONITORED
		);
		
		clnBaseAx.monitor(srvAid);
		Message cmsg = clnBaseAx.recv(AtomCode.MONITOR);
		
		// 测试多次link
		for (int i=0; i<3; ++i){
			clnBaseAx.monitor(srvAid);
			clnBaseAx.recv(cmsg, AtomCode.MONITOR);
		}
		
		clnBaseAx.send(srvAid, "INIT");
		final int pipeline = 1000;
		for (int i=0; i<pipeline; ++i){
//			clnBaseAx.send(srvAid, "HELLO", "Hello World!");
			clnBaseAx.csend(srvAid, new Hello("Hello World!"));
//			Strand.sleep(3000);
		}
		for (int i=0; i<pipeline; ++i){
			clnBaseAx.recv(cmsg, "HI");
		}
		clnBaseAx.send(srvAid, "HELLO", "end");
		
		Message msg = clnBaseAx.recv(cmsg, "OK");
		assertTrue(ActorId.equals(msg.getSender(), srvAid));
		String reply = msg.getString();
		assertTrue(StringUtils.equals("Bye!", reply));
		
		ActorExit axExit = clnBaseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		
		axExit = srvBaseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
	
		clnAxs.removeRemote("AXSSRV");
		clnAxs.shutdown();
		srvAxs.shutdown();
		
		long eclipse = System.nanoTime() - bt;
		System.out.println(idx + " done.");
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}

}
