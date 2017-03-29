/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IThreadActorHandler;
import actorx.Message;
import actorx.Guard;
import actorx.Packet;

/**
 * @author Xiong
 * 测试Actor相互发pingpong消息
 */
public class PingPong {

	private static final int count = 10000;
	
	@Test
	public void test(){
		ActorSystem ctx = new ActorSystem("AXS");
		ctx.startup();

		Actor baseAx = ctx.spawn();
		ActorId aid = ctx.spawn(baseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self){
				boolean goon = true;
				while (goon){
					try (Guard guard = self.recv("PINGPONG", "END")){
						Message msg = guard.get();
						ActorId sender = msg.getSender();
						String type = msg.getType();
						self.send(sender, msg);
						if ("END".equals(type)){
							goon = false;
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		});
		
		long bt = System.currentTimeMillis();
		Packet pkt = new Packet();
		for (int i=0; i<count; ++i){
			baseAx.send(aid, "PINGPONG", i);
			baseAx.recvPacket(pkt, "PINGPONG");
			int echo = pkt.getInt();
			assertTrue(echo == i);
		}
		baseAx.send(aid, "END");
		baseAx.recvPacket(pkt, "END");
		assertTrue(pkt.getSender().equals(aid));
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.printf("Eclipse time: %d ms\n", eclipse);
		
		ctx.shutdown();
	}

}
