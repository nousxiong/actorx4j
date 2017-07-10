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
import actorx.AtomCode;
import actorx.ExitType;
import actorx.IFiberActorHandler;
import actorx.IThreadActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.Pattern;
import actorx.util.StringUtils;

/**
 * @author Xiong
 *
 */
public class ActorBase {

	@Test
	public void test() throws InterruptedException {
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
					Message msg = self.recv(cmsg, patt);
					if (msg == null){
						// 超时
						continue;
					}
					
					String str = msg.getString();
					if (AtomCode.equals("end", str)){
						System.out.println("Recv<"+str+">");
						goon = false;
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
			baseAx.send(aid, "HELLO", "Hello World!");
		}
		baseAx.send(aid, "HELLO", "end");
		
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
	public void testFiber() throws InterruptedException {
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
					Message msg = self.recv(cmsg, patt);
					if (msg == null){
						// 超时
						continue;
					}

					String str = msg.getString();
					if (AtomCode.equals("end", str)){
						System.out.println("Recv<"+str+">");
						goon = false;
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
			baseAx.send(aid, "HELLO", "Hello World!");
		}
		baseAx.send(aid, "HELLO", "end");
		
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
