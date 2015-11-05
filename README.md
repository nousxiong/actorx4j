# actorx-java
An actor-model based communication library(Java version)

Usage
--------

Just import to eclipse.

Example
--------

```java
import actorx.Actor;
import actorx.ActorId;
import actorx.AlreadyQuitedException;
import actorx.Context;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageType;
import actorx.AbstractHandler;

public class Example {
	public void test() throws AlreadyQuitedException {
		Context ctx = Context.getInstance();
		ctx.startup();
	
		Actor base = ctx.spawn();
		ActorId aid = ctx.spawn(base, new AbstractHandler() {
			@Override
			public void run(Actor self) throws AlreadyQuitedException {
				ActorId sender = null;
				while (true){
					Message msg = self.match("init").recv(3000);
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
		assert(msg.getSender().equals(aid));
		String reply = msg.get(0);
		assert(reply.equals("Hi!"));
		
		msg = base.match(MessageType.EXIT).recv();
		assert(msg.getSender().equals(aid));
		
		ctx.join();
		
		System.out.println("done.");
	}
}
```
