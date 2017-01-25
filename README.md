# actorx4j
An actor-model based communication library(Java version)

Usage
--------

Just import to eclipse.

Dependencies
--------

https://github.com/nousxiong/cque4j

Example
--------

```java
/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.Context;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageType;
import actorx.AbstractHandler;

/**
 * @author Xiong
 *
 */
public class ActorBase {

	@Test
	public void test(){
		Context ctx = Context.getInstance();
		ctx.startup();

		Actor base = ctx.spawn();
		ActorId aid = ctx.spawn(base, new AbstractHandler() {
			@Override
			public void run(Actor self){
				ActorId sender = null;
				while (true){
					Message msg = self.match("init").recv(3000, TimeUnit.MILLISECONDS);
					if (msg != null){
						sender = msg.getSender();
						String str = msg.get(0);
						if (str.equals("end")){
							System.out.println("Recv<"+str+">");
							break;
						}
					}
				}
				self.send(sender, "ok", "Hi!");
			}
		}, 
		LinkType.MONITORED
		);
		
		for (int i=0; i<100; ++i){
			base.send(aid, "init", "Hello World!");
		}
		base.send(aid, "init", "end");
		Message msg = base.match("ok").recv();
		assertTrue(msg.getSender().equals(aid));
		String reply = msg.get(0);
		assertTrue(reply.equals("Hi!"));
		
		msg = base.match(MessageType.EXIT).recv();
		assertTrue(msg.getSender().equals(aid));
		
		ctx.join();
		
		System.out.println("done.");
	}
}

```
