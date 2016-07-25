/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import actorx.AbstractHandler;
import actorx.Actor;
import actorx.ActorId;
import actorx.AxService;
import actorx.CowBufferPool;
import actorx.MessagePool;
import actorx.Packet;

/**
 * @author Xiong
 *
 */
public class ActorLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final int count = 100000;
	
	@Test
	public void test() {
		System.out.println("Concurrent count: "+concurr);
		MessagePool.init(count * concurr / 10, Integer.MAX_VALUE);
		CowBufferPool.init(count * concurr / 10, Integer.MAX_VALUE);
		
		AxService axs = new AxService("AXS");
		axs.startup(concurr);

		Actor consumer = axs.spawn();
		
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			final int index = i;
			ActorId aid = axs.spawn(consumer, new AbstractHandler() {
				@Override
				public void run(Actor self){
					Packet pkt = self.recv(Packet.NULL, "INIT");
					ActorId consAid = pkt.getSender();
					self.send(consAid, "READY");
					self.recv(pkt, "START");
					
					for (int i=0; i<count; ++i){
						self.send(consAid, "COUNT", index, i);
					}
				}
			});
			producers.add(aid);
		}
		
		Packet pkt = new Packet();
		int[] counts = new int[concurr];
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(pkt, "READY");
		}

		long bt = System.currentTimeMillis();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		for (int i=0; i<loop; ++i){
			consumer.recv(pkt);
			int index = pkt.read();
			int c = pkt.read();
			assertTrue(counts[index] == c);
			counts[index] = ++c;
		}
		
		long eclipse = System.currentTimeMillis() - bt;
		axs.shutdown();
		
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}
}
