/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import cque.INode;
import cque.INodeFactory;
import actorx.AbstractHandler;
import actorx.Actor;
import actorx.ActorId;
import actorx.Context;
import actorx.Message;

/**
 * @author Xiong
 * 测试gc情况，使用jstat等查看不停在loop的actor
 */
public class ActorGC {

	private static final int concurr = 2;
//	private static final long count = Long.MAX_VALUE;
	private static final long count = 1;
	
	@Test
	public void test() {
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
					for (long i=0; i<count; ++i){
						Message m = self.recv();
						self.send(sender, m);
					}
				}
			});
			producers.add(aid);
		}
		
		for (ActorId aid : producers){
			consumer.send(aid, "init");
		}

		Message[] msgs = new Message[concurr];
		for (int i=0; i<concurr; ++i){
			msgs[i] = new Message();
		}
		for (long i=0; i<count; ++i){
			for (int n=0; n<concurr; ++n){
				consumer.send(producers.get(n), msgs[n]);
				assertTrue(consumer.recv() == msgs[n]);
			}
		}
		
		ctx.join();
	}

}
