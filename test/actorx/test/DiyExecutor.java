/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IThreadActorHandler;
import actorx.Message;
import actorx.Guard;
import actorx.MessagePool;
import actorx.Packet;

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
			Packet pkt = self.recvPacket("INIT");
			ActorId consAid = pkt.getSender();
			self.send(consAid, "READY");
			self.recvPacket(pkt, "START");
			
			for (int i=0; i<count; ++i){
				self.send(consAid, "COUNT", index, i);
			}
		}
		
	}
	
	@Test
	public void test() {
		// Diy executor
		ExecutorService executor = Executors.newCachedThreadPool();

		System.out.println("Concurrent count: "+concurr);
		MessagePool.init(count, Integer.MAX_VALUE, concurr + 1);
		
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup(concurr);

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
		
		Packet pkt = new Packet();
		int[] counts = new int[concurr];
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recvPacket(pkt, "READY");
		}

		long bt = System.currentTimeMillis();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		for (int i=0; i<loop; ++i){
			try (Guard guard = consumer.recv()){
				Message msg = guard.get();
				int index = msg.getInt();
				int c = msg.getInt();
				assertTrue(counts[index] == c);
				counts[index] = ++c;
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	
		long eclipse = System.currentTimeMillis() - bt;
		axs.shutdown();
		
		System.out.printf("Eclipse time: %d ms\n", eclipse);
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
		System.out.println("done.");
	}

}
