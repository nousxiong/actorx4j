/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import actorx.AbstractHandler;
import actorx.Actor;
import actorx.ActorId;
import actorx.Context;
import actorx.Message;

/**
 * @author Xiong
 *
 */
public class ActorLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final long count = 100000;
	private static final int testCount = 10;
	
	@Test
	public void test() {
		System.out.println("Concurrent count: "+concurr);
		long eclipse = loop();
		for (int i=0; i<testCount - 1; ++i){
			eclipse += loop();
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}

	private long loop(){
		Context ctx = Context.getInstance();
		ctx.startup();

		Actor consumer = ctx.spawn();
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = ctx.spawn(consumer, new AbstractHandler() {
				@Override
				public void run(Actor self){
					Message msg = self.match("init").recv();
					ActorId sender = msg.getSender();
					for (int i=0; i<count; ++i){
						self.send(sender);
					}
				}
			});
			producers.add(aid);
		}
		
		long bt = System.currentTimeMillis();
		for (ActorId aid : producers){
			consumer.send(aid, "init");
		}
		
		long loop = count * concurr;
		for (int i=0; i<loop; ++i){
			Message data = consumer.recv();
			assertTrue(data != null);
		}
		
		long eclipse = System.currentTimeMillis() - bt;
		ctx.join();
		return eclipse;
	}
}
