/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;

import actorx.util.CopyOnWriteBuffer;
import actorx.util.CowBufferFactory;
import cque.ConcurrentNodePool;
import cque.MpscNodePool;

/**
 * @author Xiong
 * 写时拷贝Buffer池
 */
public class CowBufferPool {
	// 根据容量进行分组
	private static final int BUFFER_POOL_COUNT = 26;
	private static final Integer[] capacityArray = new Integer[BUFFER_POOL_COUNT];
	private static final int DEFAULT_MAX_CACHED_BUFFER_SIZE = 1 << 20; // 1024KB
	private static final int DEFAULT_INIT_DECR_FACTOR = 10;
	private static final int DEFAULT_MAX_DECR_FACTOR = 10;
	
	/** 写时拷贝Buffer节点池 */
	private static final List<ConcurrentNodePool<CopyOnWriteBuffer>> cpoolArray = 
		new ArrayList<ConcurrentNodePool<CopyOnWriteBuffer>>(BUFFER_POOL_COUNT);
	
	static {
		// 建立buffer的capacity 数组，j从7开始的原因是最小分配长度是128（1 << 7）
		for (int i=0, j=7; i<capacityArray.length - 1; ++i, ++j){
			capacityArray[i] = 1 << j;
		}
		capacityArray[capacityArray.length - 1] = Integer.MAX_VALUE;
		
		for (int capacity : capacityArray){
			cpoolArray.add(new ConcurrentNodePool<CopyOnWriteBuffer>(new CowBufferFactory(capacity)));
		}
	}
	
	private static int maxCachedBufferSize = DEFAULT_MAX_CACHED_BUFFER_SIZE;
	
	/**
	 * 初始化size
	 * @param initCount
	 * @param maxCount
	 */
	public static void init(int initCount, int maxCount){
		init(initCount, DEFAULT_INIT_DECR_FACTOR, maxCount, DEFAULT_MAX_DECR_FACTOR);
	}
	
	/**
	 * 初始化size
	 * @param initCount
	 * @param initDecrFactor
	 * @param maxCount
	 * @param maxDecrFactor
	 */
	public static void init(int initCount, int initDecrFactor, int maxCount, int maxDecrFactor){
		int maxCachedIndex = getPoolIndex(maxCachedBufferSize);
		for (int i=0; i<capacityArray.length; ++i){
			ConcurrentNodePool<CopyOnWriteBuffer> cpool = cpoolArray.get(i);
			if (i <= maxCachedIndex){
				cpool.setInitSize(initCount);
			}
			cpool.setMaxSize(maxCount);
			initCount /= initDecrFactor;
			maxCount /= maxDecrFactor;
		}
	}
	
	/**
	 * 从池中分配一个最小容量写时拷贝Buffer
	 * @return 不会为null
	 */
	public static CopyOnWriteBuffer get(){
		MpscNodePool<CopyOnWriteBuffer> pool = getLocalPool();
		return get(pool);
	}
	
	/**
	 * 从池中分配一个指定大小的写时拷贝Buffer
	 * @return 不会为null
	 */
	public static CopyOnWriteBuffer get(int requestedCapacity){
		MpscNodePool<CopyOnWriteBuffer> pool = getLocalPool(requestedCapacity);
		return get(requestedCapacity, pool);
	}
	
	/**
	 * 使用用户之前缓存的池来分配一个写时拷贝Buffer
	 * @param pool
	 * @return 不会为null
	 */
	public static CopyOnWriteBuffer get(MpscNodePool<CopyOnWriteBuffer> pool){
		assert pool == getLocalPool();
		return cpoolArray.get(0).get(pool);
	}
	
	/**
	 * 使用用户之前缓存的池分配一个指定大小的写时拷贝Buffer
	 * @param requestedCapacity
	 * @param pool
	 * @return
	 */
	public static CopyOnWriteBuffer get(int requestedCapacity, MpscNodePool<CopyOnWriteBuffer> pool){
		if (requestedCapacity > maxCachedBufferSize){
			return new CopyOnWriteBuffer(requestedCapacity);
		}
		
		assert pool == getLocalPool(requestedCapacity);
		int index = getPoolIndex(requestedCapacity);
		if (index < 0){
			return new CopyOnWriteBuffer(requestedCapacity);
		}
		return cpoolArray.get(index).get(pool);
	}
	
	/**
	 * 取得本地线程的池
	 * @return
	 */
	public static MpscNodePool<CopyOnWriteBuffer> getLocalPool(){
		return cpoolArray.get(0).getLocalPool();
	}
	
	public static MpscNodePool<CopyOnWriteBuffer> getLocalPool(int requestedCapacity){
		if (requestedCapacity > maxCachedBufferSize){
			return null;
		}
		
		int index = getPoolIndex(requestedCapacity);
		if (index < 0){
			return null;
		}
		return cpoolArray.get(index).getLocalPool();
	}
	
	private static int getPoolIndex(int capacity){
		for (int i=0; i<BUFFER_POOL_COUNT; ++i){
			if (capacity <= capacityArray[i]){
				return i;
			}
		}
		
		return -1;
	}
}
