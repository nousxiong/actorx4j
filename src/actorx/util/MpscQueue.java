/**
 * 
 */
package actorx.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Xiong
 */
public class MpscQueue<E> {
	private ConcurrentLinkedQueue<E> que = new ConcurrentLinkedQueue<E>();
	private AtomicBoolean blocked = new AtomicBoolean(false);

	public void add(E e){
		que.add(e);
	}

	/**
	 * @return E or null
	 */
	public E poll(){
		return que.poll();
	}
	
	/**
	 * @param e
	 */
	public void put(E e){
		add(e);
		// 如果发现单读线程在阻塞，唤醒它
		if (isBlocked()){
			synchronized (que){
				if (isBlocked()){
					que.notify();
				}
			}
		}
	}
	
	/**
	 * 警告：只能在单读线程调用
	 * @return E or null
	 */
	public E take(){
		assert(!isBlocked());
		E e = poll();
		if (e != null){
			return e;
		}

		synchronized (que){
			block();
			e = poll();
			while (e == null){
				try{
					que.wait();
				}catch (InterruptedException ex){}
				
				e = poll();
			}
		}

		unblock();
		return e;
	}
	
	/**
	 * 警告：只能在单读线程调用
	 * @param timeout
	 * @return E or null
	 */
	public E poll(long timeout){
		assert(!isBlocked());
		if (timeout <= 0){
			return poll();
		}
		
		if (timeout == Long.MAX_VALUE){
			return take();
		}
		
		E e = poll();
		if (e != null){
			return e;
		}

		synchronized (que){
			block();
			e = poll();
			long currTimeout = timeout;
			while (e == null){
				long bt = System.currentTimeMillis();
				try{
					que.wait(currTimeout);
				}catch (InterruptedException ex){}
				long eclipse = System.currentTimeMillis() - bt;
				
				e = poll();
				if (eclipse >= currTimeout){
					break;
				}
				
				currTimeout -= eclipse;
			}
		}

		unblock();
		return e;
	}
	
	private boolean isBlocked(){
		return blocked.get();
	}
	
	private void block(){
		blocked.set(true);
	}
	
	private void unblock(){
		blocked.set(false);
	}
}
