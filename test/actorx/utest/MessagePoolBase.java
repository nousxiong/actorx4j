/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IFiberActorHandler;
import actorx.IThreadActorHandler;
import actorx.Message;
import actorx.util.MessagePool;

/**
 * @author Xiong
 *
 */
public class MessagePoolBase {

	static final int concurr = Runtime.getRuntime().availableProcessors();
	static final int count = 100000;
	
	@Test
	public void test(){
		MessagePool.init(1, count*concurr, count*concurr);
		ActorSystem ctx = new ActorSystem(concurr);
		ctx.startup();
		
		Actor baseAx = ctx.spawn();
		ActorId[] producers = new ActorId[concurr];
		for (int i=0; i<concurr; ++i){
			producers[i] = ctx.spawn(baseAx, new IThreadActorHandler() {
				public void run(Actor self) throws Exception{
					Message cmsg = self.recv("INIT");
					ActorId sender = cmsg.getSender();
					self.send(sender, "READY");
					self.recv(cmsg, "START");
					
					Message msg = new Message();
					msg.setType("TEST");
					for (int i=0; i<count; ++i){
						self.send(sender, msg);
					}
				}
			});
		}
		
		Message cmsg = new Message();
		for (ActorId aid : producers){
			baseAx.send(aid, "INIT");
			baseAx.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			baseAx.send(aid, "START");
		}
		
		for (int i=0; i<concurr*count; ++i){
			Message msg = baseAx.recv(cmsg);
			assertTrue(msg != null);
			assertTrue("TEST".equals(msg.getType()));
		}
		long eclipse = System.nanoTime() - bt;
		System.out.printf("Eclipse time: %d ms\n", TimeUnit.NANOSECONDS.toMillis(eclipse));
		
		ctx.shutdown();
	}

	@Test
	public void testFiber(){
		MessagePool.init(1, count*concurr, count*concurr);
		ActorSystem ctx = new ActorSystem(concurr);
		ctx.startup();
		
		Actor baseAx = ctx.spawn();
		ActorId[] producers = new ActorId[concurr];
		for (int i=0; i<concurr; ++i){
			producers[i] = ctx.spawn(baseAx, new IFiberActorHandler() {
				public void run(Actor self) throws SuspendExecution, Exception{
					Message cmsg = self.recv("INIT");
					ActorId sender = cmsg.getSender();
					self.send(sender, "READY");
					self.recv(cmsg, "START");
					
					Message msg = new Message();
					msg.setType("TEST");
					for (int i=0; i<count; ++i){
						self.send(sender, msg);
					}
				}
			});
		}
		
		Message cmsg = new Message();
		for (ActorId aid : producers){
			baseAx.send(aid, "INIT");
			baseAx.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			baseAx.send(aid, "START");
		}
		
		for (int i=0; i<concurr*count; ++i){
			Message msg = baseAx.recv(cmsg);
			assertTrue(msg != null);
			assertTrue("TEST".equals(msg.getType()));
		}
		long eclipse = System.nanoTime() - bt;
		System.out.printf("Eclipse time: %d ms\n", TimeUnit.NANOSECONDS.toMillis(eclipse));
		
		ctx.shutdown();
	}
}
