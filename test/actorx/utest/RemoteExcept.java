/**
 * 
 */
package actorx.utest;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import actorx.AbstractCustomMessage;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ActorSystemConfig;
import actorx.AtomCode;
import actorx.ExitType;
import actorx.ICustomMessageFactory;
import actorx.IFiberActorHandler;
import actorx.LinkType;
import actorx.remote.LocalInfo;
import actorx.remote.ProtocolType;
import actorx.remote.RemoteInfo;
import adata.Stream;
import amina.core.buffer.PooledBufferAllocator;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;

/**
 * @author Xiong
 *
 */
public class RemoteExcept {
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
		System.out.println("Remote except test begin...");
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("Thread test all done, average eclipse: "+eclipse);
	}

	long handleTest(int idx) throws SuspendExecution, InterruptedException {
		final String SRV_ADDRESS = "tcp://127.0.0.1:23333";
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
		
		ActorId srvAid = srvAxs.spawn(srvBaseAx, new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception {
				
			}
		}, 
		LinkType.MONITORED
		);
		ActorExit axExit = srvBaseAx.recvExit(srvAid);
		assertTrue(axExit.getExitType() == ExitType.NORMAL);
		
		// 测试链接到已经退出的actor
		clnBaseAx.monitor(srvAid);
		axExit = clnBaseAx.recvExit(srvAid);
		assertTrue(axExit.getExitType() == ExitType.EXITED);
		
		// 测试网络异常断开
		srvAid = srvAxs.spawn(srvBaseAx, new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception {
				self.recv("QUIT");
			}
		}, 
		LinkType.MONITORED
		);
		
		for (int i=0; i<10; ++i){
			clnBaseAx.monitor(srvAid);
			clnBaseAx.recv(AtomCode.MONITOR);
			// 网络断开
			clnAxs.removeRemote("AXSSRV");
			
			axExit = clnBaseAx.recvExit(srvAid);
			assertTrue(axExit.getExitType() == ExitType.NETERR);
			
			clnAxs.addRemote("AXSSRV", new RemoteInfo().parseRemote(ProtocolType.MINA_TCP, SRV_ADDRESS));
		}
		
		srvBaseAx.send(srvAid, "QUIT");
		axExit = srvBaseAx.recvExit(srvAid);
		assertTrue(axExit.getExitType() == ExitType.NORMAL);

		clnAxs.removeRemote("AXSSRV");
		clnAxs.shutdown();
		srvAxs.shutdown();
		
		long eclipse = System.nanoTime() - bt;
		if (idx != -1){
			System.out.println(idx + " done.");
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
}
