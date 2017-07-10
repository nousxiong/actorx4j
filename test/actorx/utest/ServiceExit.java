/**
 * 
 */
package actorx.utest;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.BeforeClass;
import org.junit.Test;

import actorx.AbstractCustomMessage;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ActorSystemConfig;
import actorx.AtomCode;
import actorx.IFiberActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.remote.LocalInfo;
import actorx.remote.ProtocolType;
import actorx.remote.RemoteInfo;
import actorx.util.ServiceUtils;
import adata.Stream;
import amina.core.buffer.PooledBufferAllocator;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandLocalRandom;
import cque.util.PoolGuard;

/**
 * @author Xiong
 *
 */
public class ServiceExit {
	
	public static class End extends AbstractCustomMessage {
		public static final String MTYPE = "END";
		public static final String CTYPE = "svc.exit.End";
		private String tag = "tag";

		public End() {
			super(CTYPE);
			super.setType(MTYPE);
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

		@Override
		protected void cassign(AbstractCustomMessage src){
			End msg = (End) src;
			this.tag = msg.tag;
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
		System.out.println("Service exit test begin...");
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("Thread test all done, average eclipse: "+eclipse);
	}

	void spawnEchoService(Actor sireAx){
		ActorSystem srvAxs = sireAx.getActorSystem();
		ActorId aid = srvAxs.spawn(sireAx, "ECHO", new IFiberActorHandler () {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception {
				Pattern patt = new Pattern();
				patt.match("REQ", End.MTYPE).after(10);
				
				boolean goon = true;
				while (goon){
					try (PoolGuard guard = self.precv(patt)){
						Message msg = guard.get();
						int r = StrandLocalRandom.current().nextInt(0, 99);
						if (r < 30){
							goon = false;
						}else if (msg != null){
							if (msg instanceof End){
								goon = false;
							}else{
								self.send(msg.getSender(), "RESP", msg.getString());
								if (r < 50){
									goon = false;
								}
							}
						}
					}catch (InterruptedException e){
						return;
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		}, 
		LinkType.MONITORED
		);
		assertTrue(aid != null);
	}
	
	static final ActorId echoSvcAid = ServiceUtils.makeServiceId("AXSSRV", "ECHO");
	static final ActorId monitorSvcAid = ServiceUtils.makeServiceId("AXSSRV", "MONITOR");

	void linkToEchoService(Actor ax) throws SuspendExecution, InterruptedException{
		Message cmsg = new Message();
		while (true){
			ax.monitor(echoSvcAid);
			Message msg = ax.recv(cmsg, AtomCode.EXIT, AtomCode.MONITOR);
			if (AtomCode.equals(msg.getType(), AtomCode.MONITOR)){
				break;
			}
			Strand.sleep(10);
		}
	}
	
	long handleTest(int idx) throws SuspendExecution, InterruptedException {
		final String SRV_ADDRESS = "tcp://127.0.0.1:23333";
		ActorSystem clnAxs = new ActorSystem(
			new ActorSystemConfig().setAxid("AXSCLN")
				.addCustomMessage(End.CTYPE, End.class)
			);
		ActorSystem srvAxs = new ActorSystem(
			new ActorSystemConfig().setAxid("AXSSRV")
				.addCustomMessage(End.CTYPE, End.class)
			);
		
		srvAxs.startup();
		clnAxs.startup();

		clnAxs.addRemote("AXSSRV", new RemoteInfo().parseRemote(ProtocolType.MINA_TCP, SRV_ADDRESS));
		srvAxs.listen(new LocalInfo().parseLocal(ProtocolType.MINA_TCP, SRV_ADDRESS));
		Strand.sleep(100);
		
		long bt = System.nanoTime();
		Actor clnBaseAx = clnAxs.spawn();
		Actor srvBaseAx = srvAxs.spawn("BASE");
		ActorId monitorAid = srvAxs.spawn(srvBaseAx, "MONITOR", new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception {
				spawnEchoService(self);
				
				Pattern patt = new Pattern();
				patt.match(AtomCode.EXIT, End.MTYPE);
				boolean goon = true;
				while (goon){
					try (PoolGuard guard = self.precv(patt)){
						Message msg = guard.get();
						if (msg instanceof End){
							self.crelay(echoSvcAid, (End) msg);
//							self.csend(echoSvcAid, new End());
//							self.send(echoSvcAid, "END");
							self.recvExit(echoSvcAid);
							self.send(msg.getSender(), "OK");
//							self.recvExit();
							goon = false;
						}else{
							spawnEchoService(self);
						}
					}catch (InterruptedException e){
						return;
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		}, 
		LinkType.MONITORED
		);
		assertTrue(ActorId.equals(monitorAid, monitorSvcAid));
		
		Message cmsg = new Message();
		linkToEchoService(clnBaseAx);
		for (int i=0; i<10; ++i){
			clnBaseAx.send(echoSvcAid, "REQ", "Hello, this is num "+i);
			Message msg = clnBaseAx.recv(cmsg, AtomCode.EXIT, "RESP");
			if (AtomCode.equals(AtomCode.EXIT, msg.getType())){
				linkToEchoService(clnBaseAx);
			}else{
				System.out.println("Echo: "+msg.getString());
			}
		}
		clnBaseAx.csend(monitorSvcAid, new End());
		clnBaseAx.recv(monitorSvcAid, "OK");
		
		clnAxs.removeRemote("AXSSRV");
		clnAxs.shutdown();
		
		srvBaseAx.recvExit(monitorSvcAid);
		srvAxs.shutdown();
		
		long eclipse = System.nanoTime() - bt;
		System.out.println(idx + " done.");
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
}
