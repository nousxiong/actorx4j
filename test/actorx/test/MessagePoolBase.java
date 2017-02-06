/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.IActorHandler;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.Message;
import actorx.MessageGuard;
import actorx.MessagePool;
import actorx.Packet;

/**
 * @author Xiong
 *
 */
public class MessagePoolBase {

	static final int concurr = Runtime.getRuntime().availableProcessors();
	static final int count = 100000;
	
	@Test
	public void test(){
		MessagePool.init(count, Integer.MAX_VALUE, concurr + 1);
		ActorSystem ctx = new ActorSystem("AXS");
		ctx.startup(concurr);
		
		Actor baseAx = ctx.spawn();
		ActorId[] producers = new ActorId[concurr];
		for (int i=0; i<concurr; ++i){
			producers[i] = ctx.spawn(baseAx, new IActorHandler() {
				public void run(Actor self){
					Packet pkt = self.recvPacket("INIT");
					ActorId sender = pkt.getSender();
					self.send(sender, "READY");
					self.recvPacket(pkt, "START");
					
					try (MessageGuard guard = self.makeMessage()){
						Message msg = guard.get();
						msg.setType("TEST");
						for (int i=0; i<count; ++i){
							self.send(sender, msg);
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			});
		}
		
		for (ActorId aid : producers){
			baseAx.send(aid, "INIT");
			baseAx.recvPacket("READY");
		}

		long bt = System.currentTimeMillis();
		for (ActorId aid : producers){
			baseAx.send(aid, "START");
		}
		
		for (int i=0; i<concurr*count; ++i){
			try (MessageGuard guard = baseAx.recv()){
				Message msg = guard.get();
				assertTrue(msg != null);
				assertTrue("TEST".equals(msg.getType()));
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		long eclipse = System.currentTimeMillis() - bt;
		System.out.printf("Eclipse time: %d ms\n", eclipse);
		
		ctx.shutdown();
	}

}
