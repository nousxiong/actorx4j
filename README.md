# actorx4j
An actor-model based communication library(Java version)

Usage
--------

Add dist and lib's jars into your java project

Dependencies
--------

https://github.com/nousxiong/cque4j 
https://github.com/lordoffox/adata


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
import actorx.ActorExit;
import actorx.ActorId;
import actorx.AxService;
import actorx.ExitType;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageGuard;
import actorx.AbstractHandler;
import actorx.Packet;
import actorx.Pattern;

/**
 * @author Xiong
 *
 */
public class ActorBase {

	@Test
	public void test(){
		AxService axs = new AxService("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new AbstractHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Packet pkt = self.recv(Packet.NULL, "INIT");
				ActorId baseAid = pkt.getSender();
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HELLO");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				while (true){
					try (MessageGuard guard = self.recv(patt)){
						Message msg = guard.get();
						if (msg == null){
							// 超时
							continue;
						}
						
						String str = msg.getString();
						if ("end".equals(str)){
							System.out.println("Recv<"+str+">");
							break;
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
				self.send(baseAid, "OK", "Bye!");
				
				// 测试异常退出
				throw new Exception("test");
			}
		}, 
		LinkType.MONITORED
		);
		
		baseAx.send(aid, "INIT");
		for (int i=0; i<100; ++i){
			baseAx.send(aid, "HELLO", "Hello World!");
		}
		baseAx.send(aid, "HELLO", "end");
		
		Packet pkt = baseAx.recv(Packet.NULL, "OK");
		assertTrue(pkt.getSender().equals(aid));
		String reply = pkt.getString();
		assertTrue("Bye!".equals(reply));
		
		ActorExit aex = baseAx.recvExit();
		assertTrue(aex.getExitType() == ExitType.EXCEPT);
		System.out.println(aex.getErrmsg());
	
		axs.shutdown();
	}
}
```
