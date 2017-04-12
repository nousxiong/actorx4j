/**
 * 
 */
package actorx.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Actor;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.IFiberActorHandler;
import actorx.LinkType;
import actorx.Message;
import actorx.util.MessagePool;
import actorx.util.StringUtils;

/**
 * @author Xiong
 *
 */
public class ProfileActorMpsc {

	private final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private final int count = 100000;
	private final int loopCount = 1000;
	private final ActorSystem axs = new ActorSystem("AXS");
	private Actor consumer;
	private final List<ActorId> producers = new ArrayList<ActorId>(concurr);
	private final Message cmsg = new Message();
//	private final int[] counts = new int[concurr];

	void run(){
		MessagePool.init(1, count * concurr, count * concurr);
		axs.startup(concurr);
		
		consumer = axs.spawn();
		for (int i=0; i<concurr; ++i){
//			final int idx = i;
//			ActorId aid = axs.spawn(consumer, new IThreadActorHandler() {
//			ActorId aid = axs.spawn(consumer, fibSche, new IFiberActorHandler() {
			ActorId aid = axs.spawn(consumer, new IFiberActorHandler() {
				@Override
//				public void run(Actor self) throws Exception{
				public void run(Actor self) throws SuspendExecution, Exception{
					Message cmsg = self.recv("INIT");
					ActorId consAid = cmsg.getSender();
					self.send(consAid, "READY");
					
					while (true){
						Message msg = self.recv(cmsg, "START", "END");
						if (StringUtils.equals(msg.getType(), "END")){
							break;
						}
						
						for (int i=0; i<count; ++i){
//							self.send(consAid, "COUNT", idx, i);
							self.send(consAid, "COUNT");
						}
						self.send(consAid, "STOP");
					}
				}
			}, LinkType.MONITORED);
			producers.add(aid);
		}
		
		for (ActorId aid : producers){
			consumer.send(aid, "INIT");
			consumer.recv(cmsg, "READY");
		}
		
		handleTest(-1);
		long eclipse = handleTest(0);
		for (int i=1; i<loopCount; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		
		for (ActorId aid : producers){
			consumer.send(aid, "END");
		}
		
		for (int i=0; i<producers.size(); ++i){
			consumer.recvExit();
		}
		
		axs.shutdown();
		System.out.println("all done, average eclipse: "+TimeUnit.NANOSECONDS.toMillis(eclipse));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ProfileActorMpsc().run();
	}
	
	long handleTest(int testIndex){
//		Arrays.fill(counts, 0);
		
		long bt = System.nanoTime();
		for (ActorId aid : producers){
			consumer.send(aid, "START");
		}
		
		int loop = count * concurr;
		for (int i=0; i<loop; ++i){
//			Message msg = consumer.recv(cmsg, "COUNT");
			consumer.recv(cmsg, "COUNT");
//			int idx = msg.getInt();
//			int c = msg.getInt();
//			assertTrue(counts[idx] == c);
//			counts[idx] = ++c;
		}
	
		for (int i=0; i<producers.size(); ++i){
			consumer.recv(cmsg, "STOP");
		}
		long eclipse = System.nanoTime() - bt;
		
		if (testIndex != -1){
			System.out.printf("Test %d done.\n", testIndex);
		}
		return eclipse;
	}
}
