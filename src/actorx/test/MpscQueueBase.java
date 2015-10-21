/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * @author Xiong
 */
public class MpscQueueBase {
	class Data {
		@SuppressWarnings("unused")
		private int id = 0;
		@SuppressWarnings("unused")
		private String name = "uname";
		@SuppressWarnings("unused")
		private Object[] args;
		
		public Data(int id, String name, Object... args){
			this.id = id;
			this.name = name;
			this.args = args;
		}
	}

	private static final int concurr = Runtime.getRuntime().availableProcessors() * 1;
	private static final long count = 100000;
	private static final int testCount = 30;
	private static actorx.util.MpscQueue<Data> que = new actorx.util.MpscQueue<Data>();
	@Test
	public void test() throws InterruptedException {
		System.out.println("Concurrent count: "+concurr);
		long eclipse = mpsc(false);
		for (int i=0; i<testCount - 1; ++i){
			eclipse += mpsc(false);
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}
	
	@Test
	public void testPopTimeout() throws InterruptedException{
		long bt = System.currentTimeMillis();
		Data data = que.poll(500);
		long eclipse = System.currentTimeMillis() - bt;
		assertTrue(data == null);
		assertTrue(eclipse >= 500);
	}
	
	@Test
	public void test2() throws InterruptedException{
		System.out.println("Concurrent count: "+concurr);
		long eclipse = mpsc(true);
		for (int i=0; i<testCount - 1; ++i){
			eclipse += mpsc(true);
			eclipse /= 2;
		}
		System.out.printf("Eclipse time: %d ms\n", eclipse);
	}

	private long mpsc(final boolean hasTimeout) throws InterruptedException {
		List<Thread> producers = new ArrayList<Thread>(concurr);
		
		long bt = System.currentTimeMillis();
		for (int i=0; i<concurr; ++i){
			Thread producer = new Thread() {
				@Override
				public void run(){
					for (int i=0; i<count; ++i){
						que.put(new Data(0, "My name", 1, "arg2"));
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
					if (hasTimeout){
						while (true){
							Data data = que.poll(100);
							if (data != null){
								break;
							}
						}
					}else{
						Data data = que.take();
						assertTrue(data != null);
					}
					if (--loop == 0){
						break;
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
