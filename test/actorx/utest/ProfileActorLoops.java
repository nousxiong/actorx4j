/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IFiberActorHandler;
import actorx.Message;
import actorx.util.MessagePool;

/**
 * @author Xiong
 *
 */
public class ProfileActorLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	static final ExecutorService executor = Executors.newCachedThreadPool();
	static final FiberScheduler fibSche = new FiberExecutorScheduler("AXSFIBSCHE", executor);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MessagePool.init(1, count * concurr, count * concurr);
		
		handleTest(-1);
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
		System.out.println("all done, average eclipse: "+TimeUnit.NANOSECONDS.toMillis(eclipse));
	}
	
	static long handleTest(int testIndex){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup(concurr);

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			final int idx = i;
//			ActorId aid = axs.spawn(consumer, new IThreadActorHandler() {
			ActorId aid = axs.spawn(consumer, fibSche, new IFiberActorHandler() {
				@Override
//				public void run(Actor self) throws Exception{
				public void run(Actor self) throws SuspendExecution, Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					self.recv(cmsg, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", idx, i);
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
			int idx = msg.getInt();
			int c = msg.getInt();
			assertTrue(counts[idx] == c);
			counts[idx] = ++c;
		}
	
		long eclipse = System.nanoTime() - bt;
		axs.shutdown();
		
		if (testIndex != -1){
			System.out.printf("Test %d done.\n", testIndex);
		}
		return eclipse;
	}

}
