/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.AtomCode;
import actorx.IFiberActorHandler;
import actorx.IThreadActorHandler;
import actorx.Message;

/**
 * @author Xiong
 * @creation 2016年9月29日下午3:12:01
 * 测试actor之间相互链接
 */
public class LinkBase {

	@Test
	public void test() throws InterruptedException{
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		Actor ax1 = axs.spawn();
		ax1.quit();
		baseAx.link(ax1.getActorId());
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXITED);
		baseAx.monitor(ax1.getActorId());
		axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.EXITED);
		
		Actor ax2 = axs.spawn();
		baseAx.link(ax2.getActorId());
		Message cmsg = baseAx.recv(AtomCode.LINK);
		assertTrue(cmsg != null);
		assertTrue(AtomCode.equals(cmsg.getType(), AtomCode.LINK));
		
		Set<ActorId> quiters = new HashSet<ActorId>();
		for (int i=0; i<100; ++i){
			final int index = i;
			ActorId aid = null;
			if (i % 2 == 0){
				aid = axs.spawn(new IThreadActorHandler() {
					@Override
					public void run(Actor self) throws Exception {
						if (index % 7 == 0){
							Thread.sleep(10);
						}
					}
				});
			}else{
				aid = axs.spawn(new IFiberActorHandler() {
					@Override
					public void run(Actor self) throws SuspendExecution, Exception {
						if (index % 7 == 0){
							Strand.sleep(10);
						}
					}
				});
			}
			baseAx.monitor(aid);
			quiters.add(aid);
		}
		
		for (int i=0, size=quiters.size(); i<size; ++i){
			Message msg = baseAx.recv(cmsg, AtomCode.MONITOR, AtomCode.EXIT);
			String type = msg.getType();
			if (AtomCode.equals(type, AtomCode.EXIT)){
				axExit = msg.getRaw();
				if (axExit.getExitType() == ExitType.NORMAL){
					--i;
				}
			}
		}
		
		axs.shutdown();
	}

}
