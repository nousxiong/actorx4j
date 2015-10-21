/**
 * 
 */
package actorx.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.AlreadyQuitedException;
import actorx.Context;
import actorx.AbstractHandler;
import actorx.Message;

/**
 * @author Xiong
 * 测试Actor非阻塞性能
 */
public class ActorNonblockedLoops {

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final long count = 100000;
	private static final int testCount = 30;
	
	@Test
	public void test() throws AlreadyQuitedException {
		System.out.println("Concurrent count: "+concurr);
		long eclipse = loop();
		for (int i=0; i<testCount - 1; ++i){
			eclipse += loop();
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}

	private long loop() throws AlreadyQuitedException{
		Context ctx = Context.getInstance();
		ctx.startup();

		Actor consumer = ctx.spawn();
		List<ActorId> producers = new ArrayList<ActorId>(concurr);
		for (int i=0; i<concurr; ++i){
			ActorId aid = ctx.spawn(consumer, new AbstractHandler() {
				@Override
				public void run(Actor self) throws AlreadyQuitedException {
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
		while (true){
			Message data = consumer.recv(0);
			if (data != null){
				if (--loop == 0){
					break;
				}
			}
		}
		long eclipse = System.currentTimeMillis() - bt;
		ctx.join();
		return eclipse;
	}
}
