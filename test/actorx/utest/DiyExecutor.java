/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
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
 * @creation 2017年3月23日上午9:15:45
 * 测试自定义执行器
 */
public class DiyExecutor {
	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	
	static class MyActor implements IThreadActorHandler {
		private int index;
		
		public MyActor(int index){
			this.index = index;
		}

		@Override
		public void run(Actor self) throws Exception {
			Message cmsg = self.recv("INIT");
			ActorId consAid = cmsg.getSender();
			self.send(consAid, "READY");
			self.recv(cmsg, "START");
			
			for (int i=0; i<count; ++i){
				self.send(consAid, "COUNT", index, i);
			}
		}
		
	}
	static class MyFiberActor implements IFiberActorHandler {
		private int index;
		
		public MyFiberActor(int index){
			this.index = index;
		}

		@Override
		public void run(Actor self) throws SuspendExecution, Exception {
			Message cmsg = self.recv("INIT");
			ActorId consAid = cmsg.getSender();
			self.send(consAid, "READY");
			self.recv(cmsg, "START");
			
			for (int i=0; i<count; ++i){
				self.send(consAid, "COUNT", index, i);
			}
		}
		
	}
	
	@Test
	public void test() throws InterruptedException {
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

	long handleTest(int idx) throws InterruptedException{
		// Diy executor
		ExecutorService executor = Executors.newCachedThreadPool();
		
		ActorSystem axs = new ActorSystem(concurr);
		axs.startup();

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = null;
			if (i % 2 == 0){
				aid = axs.spawn(consumer, new MyActor(i));
			}else{
				aid = axs.spawn(consumer, executor, new MyActor(i));
			}
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
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
		
		if (idx != -1){
			System.out.printf("%d done, concurrent count: %d\n", idx, concurr);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}

	long handleTestFiber(int idx) throws InterruptedException{
		// Diy executor
		ExecutorService executor = Executors.newCachedThreadPool();
		FiberScheduler fibSche = new FiberExecutorScheduler("AXSFIBSCHE", executor);
		
		ActorSystem axs = new ActorSystem(concurr);
		axs.startup();

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = null;
			if (i % 2 == 0){
				aid = axs.spawn(consumer, new MyFiberActor(i));
			}else{
				aid = axs.spawn(consumer, fibSche, new MyFiberActor(i));
			}
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
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
		
		if (idx != -1){
			System.out.printf("Fiber %d done, concurrent count: %d\n", idx, concurr);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
}
