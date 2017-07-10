/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
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
public class ActorLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	
	@Test
	public void test() throws InterruptedException{
		MessagePool.init(1, count*concurr, count*concurr);
		handleTestFiber(-1);
		long eclipse = handleTestFiber(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTestFiber(i);
			eclipse /= 2;
		}
		System.out.println("Fiber test all done, average eclipse: "+eclipse);

		eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("Thread test all done, average eclipse: "+eclipse);
	}
	
	long handleTest(int idx) throws InterruptedException{
		ActorSystem axs = new ActorSystem(concurr);
		axs.startup();

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			final int index = i;
			ActorId aid = axs.spawn(consumer, new IThreadActorHandler() {
				@Override
				public void run(Actor self) throws Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					self.recv(cmsg, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", index, i);
					}
				}
			});
			producers.add(aid);
		}
		
		Message cmsg = new Message();
		int[] counts = new int[concurr];
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		for (int i=0; i<loop; ++i){
			Message msg = consumer.recv(cmsg);
			int index = msg.getInt();
			int c = msg.getInt();
			assertTrue(counts[index] == c);
			counts[index] = ++c;
		}
	
		long eclipse = System.nanoTime() - bt;
		axs.shutdown();
		
		System.out.println(idx + " done, concurrent count: "+concurr);
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
	
	long handleTestFiber(int idx) throws InterruptedException{
		ActorSystem axs = new ActorSystem(concurr);
		axs.startup();

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			final int index = i;
			ActorId aid = axs.spawn(consumer, new IFiberActorHandler() {
				@Override
				public void run(Actor self) throws SuspendExecution, Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					self.recv(cmsg, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", index, i);
					}
				}
			});
			producers.add(aid);
		}
		
		Message cmsg = new Message();
		int[] counts = new int[concurr];
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		for (int i=0; i<loop; ++i){
			Message msg = consumer.recv(cmsg);
			int index = msg.getInt();
			int c = msg.getInt();
			assertTrue(counts[index] == c);
			counts[index] = ++c;
		}
	
		long eclipse = System.nanoTime() - bt;
		axs.shutdown();
		
		if (idx != -1){
			System.out.println(idx + " done, concurrent count: "+concurr);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
}
