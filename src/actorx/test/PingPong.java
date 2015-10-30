/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.Context;
import actorx.AbstractHandler;
import actorx.Message;

/**
 * @author Xiong
 * 测试Actor相互发pingpong消息
 */
public class PingPong {

	private static final int count = 10000;
	@Test
	public void test(){
		Context ctx = Context.getInstance();
		ctx.startup();

		Actor base = ctx.spawn();
		ActorId aid = ctx.spawn(base, new AbstractHandler() {
			@Override
			public void run(Actor self){
				while (true){
					Message msg = self.match("pingpong","end").recv();
					ActorId sender = msg.getSender();
					self.send(sender, msg.getType());
					if (msg.getType().equals("end")){
						break;
					}
				}
			}
		});
		
		long bt =System.currentTimeMillis();
		for (int i=0; i<count; ++i){
			base.send(aid, "pingpong");
			base.match("pingpong").recv();
		}
		base.send(aid, "end");
		Message msg = base.match("end").recv();
		assertTrue(msg.getSender().equals(aid));
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.printf("Eclipse time: %d ms\n", eclipse);
		
		ctx.join();
	}

}
