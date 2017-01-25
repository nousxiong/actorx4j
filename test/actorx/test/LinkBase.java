/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import actorx.Actor;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.ExitType;
import actorx.IActorHandler;
import actorx.MsgType;
import actorx.Packet;

/**
 * @author Xiong
 * @creation 2016年9月29日下午3:12:01
 * 测试actor之间相互链接
 */
public class LinkBase {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		ActorSystem axs = new ActorSystem("AXS");
		axs.startup();

		Actor baseAx = axs.spawn();
		Actor ax1 = axs.spawn();
		ax1.quit();
		baseAx.link(ax1.getActorId());
		ActorExit axe = baseAx.recvExit();
		assertTrue(axe.getExitType() == ExitType.ALREADY);
		baseAx.monitor(ax1.getActorId());
		axe = baseAx.recvExit();
		assertTrue(axe.getExitType() == ExitType.ALREADY);
		
		Actor ax2 = axs.spawn();
		baseAx.link(ax2.getActorId());
		Packet pkt = baseAx.recvPacket(MsgType.LINK);
		assertTrue(pkt != null);
		assertTrue(MsgType.equals(pkt.getType(), MsgType.LINK));
		
		Set<ActorId> quiters = new HashSet<ActorId>();
		for (int i=0; i<100; ++i){
			final int index = i;
			ActorId aid = axs.spawn(new IActorHandler() {
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
			pkt = baseAx.recvPacket(pkt, MsgType.MONITOR, MsgType.EXIT);
			String type = pkt.getType();
			if (MsgType.equals(type, MsgType.EXIT)){
				axe = pkt.getRaw();
				if (axe.getExitType() == ExitType.NORMAL){
					--i;
				}
			}
		}
		
		axs.shutdown();
	}

}
