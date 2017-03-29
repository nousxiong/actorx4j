/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.AtomCode;
import actorx.IThreadActorHandler;
import actorx.Packet;

/**
 * @author Xiong
 * @creation 2016年9月29日下午3:12:01
 * 测试actor之间相互链接
 */
public class LinkBase {

	@Test
	public void test(){
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		Actor ax1 = axs.spawn();
		ax1.quit();
		baseAx.link(ax1.getActorId());
		ActorExit axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.ALREADY);
		baseAx.monitor(ax1.getActorId());
		axExit = baseAx.recvExit();
		assertTrue(axExit.getExitType() == ExitType.ALREADY);
		
		Actor ax2 = axs.spawn();
		baseAx.link(ax2.getActorId());
		Packet pkt = baseAx.recvPacket(AtomCode.LINK);
		assertTrue(pkt != null);
		assertTrue(AtomCode.equals(pkt.getType(), AtomCode.LINK));
		
		Set<ActorId> quiters = new HashSet<ActorId>();
		for (int i=0; i<100; ++i){
			final int index = i;
			ActorId aid = axs.spawn(new IThreadActorHandler() {
				@Override
				public void run(Actor self) throws Exception {
					if (index % 7 == 0){
						Thread.sleep(10);
					}
				}
			});
			baseAx.monitor(aid);
			quiters.add(aid);
		}
		
		for (int i=0, size=quiters.size(); i<size; ++i){
			pkt = baseAx.recvPacket(pkt, AtomCode.MONITOR, AtomCode.EXIT);
			String type = pkt.getType();
			if (AtomCode.equals(type, AtomCode.EXIT)){
				axExit = pkt.getRaw();
				if (axExit.getExitType() == ExitType.NORMAL){
					--i;
				}
			}
		}
		
		axs.shutdown();
	}

}
