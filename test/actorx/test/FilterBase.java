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
import actorx.IRecvFilter;
import actorx.ISendFilter;
import actorx.IThreadActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Guard;
import actorx.Packet;
import actorx.Pattern;

/**
 * @author Xiong
 * @creation 2016年9月15日上午9:20:50
 * 测试过滤器
 */
public class FilterBase {
	
	class TypeSendFilter implements ISendFilter {

		private String newType;
		
		public TypeSendFilter(String newType){
			this.newType = newType;
		}
		
		@Override
		public Message filterSend(ActorId toAid, String type, Message prevMsg, Message srcMsg) {
			if (prevMsg != null){
				prevMsg.setType(newType);
			}
			return prevMsg;
		}
		
	}
	
	class TypeRecvFilter implements IRecvFilter {
		
		private String newType;
		
		public TypeRecvFilter(String newType){
			this.newType = newType;
		}

		@Override
		public Message filterRecv(ActorId fromAid, String type, Message prevMsg, Message srcMsg) {
			if (prevMsg != null){
				prevMsg.setType(newType);
			}
			return prevMsg;
		}
		
	}

	@Test
	public void test(){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Packet pkt = self.recvPacket("INIT");
				ActorId baseAid = pkt.getSender();
				
				self.addRecvFilter("HELLOEX", new TypeRecvFilter("HI"));
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HI");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					try (Guard guard = self.recv(patt)){
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
		
		baseAx.addSendFilter("HELLO", new TypeSendFilter("HELLOEX"));
		
		baseAx.send(aid, "INIT");
		for (int i=0; i<100; ++i){
			baseAx.send(aid, "HELLO", "Hello World!");
		}
		baseAx.send(aid, "HELLO", "end");
		
		baseAx.addRecvFilter("OK", new TypeRecvFilter("BYEBYE"));
		
		Packet pkt = baseAx.recvPacket("BYEBYE");
		assertTrue(pkt.getSender().equals(aid));
		String reply = pkt.getString();
		assertTrue("Bye!".equals(reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}

}
