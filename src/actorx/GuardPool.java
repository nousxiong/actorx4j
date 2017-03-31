/**
 * 
 */
package actorx;

import actorx.detail.GuardFactory;
import cque.ConcurrentObjectPool;

/**
 * @author Xiong
 *
 */
public class GuardPool {
	/** 守护者池 */
	private static ConcurrentObjectPool<Guard> cpool = 
		new ConcurrentObjectPool<Guard>(new GuardFactory());

	/**
	 * 初始化size
	 * @param initSize
	 * @param maxSize
	 */
	public static void init(int poolSize, int initSize, int maxSize){
		cpool = new ConcurrentObjectPool<Guard>(new GuardFactory(), poolSize, initSize, maxSize);
	}
	
	/**
	 * 从池中分配一个守护者
	 * @return 不会为null
	 */
	public static Guard borrowObject(){
		return cpool.borrowObject();
	}
}
