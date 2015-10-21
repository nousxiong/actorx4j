/**
 * 
 */
package actorx.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import actorx.Message;
import actorx.util.MpscQueue;

/**
 * @author Xiong
 * 测试MpscQueue在非阻塞模式下的性能
 */
public class MpscNonblockedLoops {
	
	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final long count = 100000;
	private static final int testCount = 30;
	private static MpscQueue<Message> que = new MpscQueue<Message>();

	@Test
	public void test() throws InterruptedException{
		System.out.println("Concurrent count: "+concurr);
		long eclipse = mpsc();
		for (int i=0; i<testCount - 1; ++i){
			eclipse += mpsc();
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}

	private long mpsc() throws InterruptedException {
		List<Thread> producers = new ArrayList<Thread>(concurr);
		
		long bt = System.currentTimeMillis();
		for (int i=0; i<concurr; ++i){
			Thread producer = new Thread() {
				@Override
				public void run(){
					for (int i=0; i<count; ++i){
						que.add(new Message(null, "My name", 1, "arg2"));
					}
				}
			};
			producers.add(producer);
			producer.start();
		}
		
		Thread consumer = new Thread() {
			@Override
			public void run(){
				long loop = count * concurr;
				while (true){
					Message data = que.poll();
					if (data != null){
						if (--loop == 0){
							break;
						}
					}
				}
			}
		};
		consumer.start();
		
		for (Thread producer : producers){
			producer.join();
		}
		consumer.join();
		
		return System.currentTimeMillis() - bt;
	}
}
