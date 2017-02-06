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
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.LinkType;
import actorx.Message;
import actorx.MessageGuard;
import actorx.IActorHandler;
import actorx.Packet;
import actorx.Pattern;

/**
 * @author Xiong
 *
 */
public class ActorBase {

	@Test
	public void test(){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Packet pkt = self.recvPacket("INIT");
				ActorId baseAid = pkt.getSender();
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HELLO");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					try (MessageGuard guard = self.recv(patt)){
						Message msg = guard.get();
						if (msg == null){
							// 超时
							continue;
						}
						
						String str = msg.getString();
						if ("end".equals(str)){
							System.out.println("Recv<"+str+">");
							goon = false;
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
		
		Packet pkt = baseAx.recvPacket("OK");
		assertTrue(pkt.getSender().equals(aid));
		String reply = pkt.getString();
		assertTrue("Bye!".equals(reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}
}
