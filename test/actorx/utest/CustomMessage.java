/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.IFiberActorHandler;
import actorx.IThreadActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.util.StringUtils;
import co.paralleluniverse.fibers.SuspendExecution;
import cque.util.PoolGuard;

/**
 * @author Xiong
 *
 */
public class CustomMessage {
	
	static class Hello extends Message {
		private String tag;
		public static final String TYPE = "HELLO";
		
		public Hello(String tag){
			super.setType(TYPE);
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		}
	}
	
	static class End extends Hello {
		public End() {
			super("end");
		}
	}

	@Test
	public void test(){
		ActorSystem axs = new ActorSystem();
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				Message cmsg = self.recv("INIT");
				ActorId baseAid = cmsg.getSender();
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HELLO");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					try(PoolGuard guard = self.precv(patt)){
						Message msg = guard.get();
						if (msg == null){
							// 超时
							continue;
						}
						
						if (msg instanceof End){
							End end = (End) msg;
							System.out.println("Recv<"+end.getTag()+">");
							goon = false;
						}else if (msg instanceof Hello){
							Hello hello = (Hello) msg;
							assertTrue(StringUtils.equals("Hello World!", hello.getTag()));
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
				self.send(baseAid, "OK", "Bye!");
				
				// 测试异常退出
				throw new Exception("thread test exception");
			}
		}, 
		LinkType.MONITORED
		);
		
		baseAx.send(aid, "INIT");
		for (int i=0; i<100; ++i){
			baseAx.csend(aid, new Hello("Hello World!"));
		}
		baseAx.csend(aid, new End());
		
		Message cmsg = baseAx.recv("OK");
		assertTrue(ActorId.equals(cmsg.getSender(), aid));
		String reply = cmsg.getString();
		assertTrue(StringUtils.equals("Bye!", reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}
	
	@Test
	public void testFiber(){
		ActorSystem axs = new ActorSystem();
		axs.startup();

		Actor baseAx = axs.spawn();
		ActorId aid = axs.spawn(baseAx, new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception{
				Message cmsg = self.recv("INIT");
				ActorId baseAid = cmsg.getSender();
				
				// 设置接收模式
				Pattern patt = new Pattern();
				patt.match("HELLO");
				patt.after(3000, TimeUnit.MILLISECONDS);
				
				boolean goon = true;
				while (goon){
					try(PoolGuard guard = self.precv(patt)){
						Message msg = guard.get();
						if (msg == null){
							// 超时
							continue;
						}
						
						if (msg instanceof End){
							End end = (End) msg;
							System.out.println("Recv<"+end.getTag()+">");
							goon = false;
						}else if (msg instanceof Hello){
							Hello hello = (Hello) msg;
							assertTrue(StringUtils.equals("Hello World!", hello.getTag()));
						}
					}catch (Exception e){
						e.printStackTrace();
					}
				}
				self.send(baseAid, "OK", "Bye!");
				
				// 测试异常退出
				throw new Exception("fiber test exception");
			}
		}, 
		LinkType.MONITORED
		);
		
		baseAx.send(aid, "INIT");
		for (int i=0; i<100; ++i){
			baseAx.csend(aid, new Hello("Hello World!"));
		}
		baseAx.csend(aid, new End());
		
		Message cmsg = baseAx.recv("OK");
		assertTrue(ActorId.equals(cmsg.getSender(), aid));
		String reply = cmsg.getString();
		assertTrue(StringUtils.equals("Bye!", reply));
		
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXCEPT);
		System.out.println(axExit.getErrmsg());
	
		axs.shutdown();
	}
}
