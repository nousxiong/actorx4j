/**
 * 
 */
package actorx;

import actorx.util.CopyOnWriteBuffer;
import actorx.util.CowBufferFactory;
import cque.ConcurrentNodePool;
import cque.MpscNodePool;

/**
 * @author Xiong
 * 写时拷贝Buffer池
 */
public class CowBufferPool {
	/** 写时拷贝Buffer节点池 */
	private static final ConcurrentNodePool<CopyOnWriteBuffer> cpool = 
		new ConcurrentNodePool<CopyOnWriteBuffer>(new CowBufferFactory());
	
	/**
	 * 初始化size
	 * @param initSize
	 * @param maxSize
	 */
	public static void init(int initSize, int maxSize){
		assert cpool != null;
		cpool.setInitSize(initSize);
		cpool.setMaxSize(maxSize);
	}
	
	/**
	 * 从池中分配一个写时拷贝Buffer
	 * @return 不会为null
	 */
	public static CopyOnWriteBuffer get(){
		MpscNodePool<CopyOnWriteBuffer> pool = getLocalPool();
		return get(pool);
	}
	
	/**
	 * 使用用户之前缓存的池来分配一个写时拷贝Buffer
	 * @param pool
	 * @return 不会为null
	 */
	public static CopyOnWriteBuffer get(MpscNodePool<CopyOnWriteBuffer> pool){
		assert pool == getLocalPool();
		return cpool.get(pool);
	}
	
	/**
	 * 取得本地线程的池
	 * @return
	 */
	public static MpscNodePool<CopyOnWriteBuffer> getLocalPool(){
		return cpool.getLocalPool();
	}
	
}
