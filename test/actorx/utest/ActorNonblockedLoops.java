/**
 * 
 */
package actorx.utest;

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
 * 测试Actor非阻塞性能
 */
public class ActorNonblockedLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	
	@Test
	public void test(){
		MessagePool.init(1, count*concurr, count*concurr);
		handleTest(-1);
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
		
		handleTestFiber(-1);
		eclipse = handleTestFiber(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTestFiber(i);
			eclipse /= 2;
		}
		System.out.printf("Fiber eclipse time: %d ms\n", eclipse);
	}

	private long handleTest(int idx){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup(concurr);

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = axs.spawn(consumer, new IThreadActorHandler() {
				@Override
				public void run(Actor self) throws Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					self.recv(cmsg, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", i);
					}
				}
			});
			producers.add(aid);
		}
		
		Message cmsg = new Message();
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		while (true){
			Message msg = consumer.recv(cmsg, 0, TimeUnit.MILLISECONDS);
			if (msg != null){
				if (--loop == 0){
					break;
				}
			}
		}
		long eclipse = System.nanoTime() - bt;
		axs.shutdown();
		
		if (idx != -1){
			System.out.printf("%d done, concurrent count: %d\n", idx, concurr);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}

	private long handleTestFiber(int idx){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup(concurr);

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = axs.spawn(consumer, new IFiberActorHandler() {
				@Override
				public void run(Actor self) throws SuspendExecution, Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					self.recv(cmsg, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", i);
					}
				}
			});
			producers.add(aid);
		}
		
		Message cmsg = new Message();
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(cmsg, "READY");
		}

		long bt = System.nanoTime();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		while (true){
			Message msg = consumer.recv(cmsg, 0, TimeUnit.MILLISECONDS);
			if (msg != null){
				if (--loop == 0){
					break;
				}
			}
		}
		long eclipse = System.nanoTime() - bt;
		axs.shutdown();
		
		if (idx != -1){
			System.out.printf("%d done, concurrent count: %d\n", idx, concurr);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
}
