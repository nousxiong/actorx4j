/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IFiberActorHandler;
import actorx.IThreadActorHandler;
import actorx.Message;
import actorx.util.MessagePool;

/**
 * @author Xiong
 * 测试Actor相互发pingpong消息
 */
public class PingPong {

	private static final int count = 10000;
	
	@Test
	public void test(){
		MessagePool.init(1, count*2, count*2);
		handleTestFiber(-1);
		long eclipse = handleTestFiber(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTestFiber(i);
			eclipse /= 2;
		}
		System.out.println("Fiber test all done, average eclipse: "+eclipse);

		eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("Thread test all done, average eclipse: "+eclipse);
	}
	
	public long handleTest(int idx){
		ActorSystem ctx = new ActorSystem("AXS");
		ctx.startup();

		Actor baseAx = ctx.spawn();
		ActorId aid = ctx.spawn(baseAx, new IThreadActorHandler() {
			@Override
			public void run(Actor self) throws Exception{
				boolean goon = true;
				Message cmsg = new Message();
				while (goon){
					Message msg = self.recv(cmsg, "PINGPONG", "END");
					ActorId sender = msg.getSender();
					String type = msg.getType();
					self.send(sender, msg);
					if ("END".equals(type)){
						goon = false;
					}
				}
			}
		});
		
		long bt = System.nanoTime();
		Message cmsg = new Message();
		for (int i=0; i<count; ++i){
			baseAx.send(aid, "PINGPONG", i);
			baseAx.recv(cmsg, "PINGPONG");
			int echo = cmsg.getInt();
			assertTrue(echo == i);
		}
		baseAx.send(aid, "END");
		baseAx.recv(cmsg, "END");
		assertTrue(ActorId.equals(cmsg.getSender(), aid));
		
		long eclipse = System.nanoTime() - bt;
		ctx.shutdown();
		
		if (idx != -1){
			System.out.printf("Thread test %d done\n", idx);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}
	
	public long handleTestFiber(int idx){
		ActorSystem ctx = new ActorSystem("AXS");
		ctx.startup();

		Actor baseAx = ctx.spawn();
		ActorId aid = ctx.spawn(baseAx, new IFiberActorHandler() {
			@Override
			public void run(Actor self) throws SuspendExecution, Exception{
				boolean goon = true;
				Message cmsg = new Message();
				while (goon){
					Message msg = self.recv(cmsg, "PINGPONG", "END");
					ActorId sender = msg.getSender();
					String type = msg.getType();
					self.send(sender, msg);
					if ("END".equals(type)){
						goon = false;
					}
				}
			}
		});
		
		long bt = System.nanoTime();
		Message cmsg = new Message();
		for (int i=0; i<count; ++i){
			baseAx.send(aid, "PINGPONG", i);
			baseAx.recv(cmsg, "PINGPONG");
			int echo = cmsg.getInt();
			assertTrue(echo == i);
		}
		baseAx.send(aid, "END");
		baseAx.recv(cmsg, "END");
		assertTrue(ActorId.equals(cmsg.getSender(), aid));
		
		long eclipse = System.nanoTime() - bt;
		ctx.shutdown();
		
		if (idx != -1){
			System.out.printf("Fiber test %d done\n", idx);
		}
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}

}
