/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.IFiberActorHandler;
import actorx.IRecvFilter;
import actorx.ISendFilter;
import actorx.IThreadActorHandler;
import actorx.LinkType;
import actorx.Message;
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
	public void test() throws InterruptedException{
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Message cmsg = self.recv("INIT");
				ActorId baseAid = cmsg.getSender();
				
				self.addRecvFilter("HELLOEX", new TypeRecvFilter("HI"));
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HI");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					Message msg = self.recv(cmsg, patt);
					if (msg == null){
						// 超时
						continue;
					}
					
					String str = msg.getString();
					if ("end".equals(str)){
						System.out.println("Recv<"+str+">");
						goon = false;
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
		
		Message cmsg = baseAx.recv("BYEBYE");
		assertTrue(cmsg.getSender().equals(aid));
		String reply = cmsg.getString();
		assertTrue("Bye!".equals(reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}
	
	@Test
	public void testFiber() throws InterruptedException{
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception{
				Message cmsg = self.recv("INIT");
				ActorId baseAid = cmsg.getSender();
				
				self.addRecvFilter("HELLOEX", new TypeRecvFilter("HI"));
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HI");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					Message msg = self.recv(cmsg, patt);
					if (msg == null){
						// 超时
						continue;
					}
					
					String str = msg.getString();
					if ("end".equals(str)){
						System.out.println("Recv<"+str+">");
						goon = false;
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
		
		Message cmsg = baseAx.recv("BYEBYE");
		assertTrue(cmsg.getSender().equals(aid));
		String reply = cmsg.getString();
		assertTrue("Bye!".equals(reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}

}
