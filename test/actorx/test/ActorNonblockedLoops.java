/**
 * 
 */
package actorx.test;

import java.util.ArrayList;
import java.util.List;
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
 * 测试Actor非阻塞性能
 */
public class ActorNonblockedLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	private static final int testCount = 1;
	
	@Test
	public void test(){
		MessagePool.init(count, Integer.MAX_VALUE, concurr + 1);
		System.out.println("Concurrent count: "+concurr);
		long eclipse = loop();
		for (int i=0; i<testCount - 1; ++i){
			eclipse += loop();
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}

	private long loop(){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup(concurr);

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = axs.spawn(consumer, new IThreadActorHandler() {
				@Override
				public void run(Actor self) throws Exception{
					Packet pkt = self.recvPacket("INIT");
					ActorId consAid = pkt.getSender();
					self.send(consAid, "READY");
					self.recvPacket(pkt, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", i);
					}
				}
			});
			producers.add(aid);
		}
		
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recvPacket("READY");
		}

		long bt = System.currentTimeMillis();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		while (true){
			try (Guard guard = consumer.recv(0, TimeUnit.MILLISECONDS)){
				Message msg = guard.get();
				if (msg != null){
					if (--loop == 0){
						break;
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		long eclipse = System.currentTimeMillis() - bt;
		axs.shutdown();
		return eclipse;
	}
}
