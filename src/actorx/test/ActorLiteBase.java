/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.ActorLite;
import actorx.Context;
import actorx.AbstractHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageType;

/**
 * @author Xiong
 * 
 */
public class ActorLiteBase {
	private class CountDown extends ActorLite {
		private Thread thr;
		public CountDown(final ActorId listener, final int countDown) {
			thr = new Thread() {
				@Override
				public void run(){
					int count = countDown;
					while (--count >= 0){
						send(listener, "cd", count);
					}
				}
			};
			thr.start();
		}
		
		void join(){
			try {
				thr.join();
			} catch (InterruptedException e) {
			}
		}
	}
	@Test
	public void test(){
		Context ctx = Context.getInstance();
		ctx.startup();
		
		Actor base = ctx.spawn();
		ctx.spawn(base, new AbstractHandler() {
			@Override
			public void run(Actor self){
				CountDown c = new CountDown(self.getActorId(), 10);
				while (true){
					Message msg = self.match("cd").recv();
					assertTrue(msg.getSender().equals(c.getActorId()));
					int count = msg.get(0);
					System.out.println("Count: "+count);
					if (count == 0){
						break;
					}
				}
				c.join();
			}
		},
		LinkType.MONITORED
		);
		base.match(MessageType.EXIT).recv();
		ctx.join();
		System.out.println("done.");
	}

}
