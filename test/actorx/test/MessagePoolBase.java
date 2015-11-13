/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.INodePool;
import actorx.AbstractHandler;
import actorx.Actor;
import actorx.ActorId;
import actorx.Context;
import actorx.Message;
import actorx.MessagePool;

/**
 * @author Xiong
 *
 */
public class MessagePoolBase {

	static final int concurr = Runtime.getRuntime().availableProcessors();
	static final int count = 100000;
	
	@Test
	public void test(){
		MessagePool.init(1000, Integer.MAX_VALUE);
		Context ctx = Context.getInstance();
		ctx.startup();
		
		Actor base = ctx.spawn();
		ActorId[] producers = new ActorId[concurr];
		for (int i=0; i<concurr; ++i){
			producers[i] = ctx.spawn(base, new AbstractHandler() {
				public void run(Actor self){
					ActorId sender = self.match("init").recv().getSender();
					INodePool pool = MessagePool.getLocalPool();
					for (int i=0; i<count; ++i){
						Message msg = MessagePool.get(pool);
						self.send(sender, msg, "test");
					}
				}
			});
		}
		
		for (ActorId aid : producers){
			base.send(aid, "init");
		}
		
		for (int i=0; i<concurr*count; ++i){
			Message msg = base.recv();
			assertTrue(msg.getType().equals("test"));
			msg.release();
		}
		
		ctx.join();
	}

}
