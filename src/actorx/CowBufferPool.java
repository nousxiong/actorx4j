/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;

import actorx.detail.CowBuffer;
import actorx.detail.CowBufferFactory;
import cque.ConcurrentObjectPool;

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
	private static final List<ConcurrentObjectPool<CowBuffer>> cpoolArray = 
		new ArrayList<ConcurrentObjectPool<CowBuffer>>(BUFFER_POOL_COUNT);
	
	static {
		// 建立buffer的capacity 数组，j从7开始的原因是最小分配长度是128（1 << 7）
		for (int i=0, j=7; i<capacityArray.length - 1; ++i, ++j){
			capacityArray[i] = 1 << j;
		}
		capacityArray[capacityArray.length - 1] = Integer.MAX_VALUE;
		
		for (int capacity : capacityArray){
			cpoolArray.add(new ConcurrentObjectPool<CowBuffer>(new CowBufferFactory(capacity)));
		}
	}
	
	private static int maxCachedBufferSize = DEFAULT_MAX_CACHED_BUFFER_SIZE;
	
	/**
	 * 初始化size
	 * @param poolCount
	 * @param initCount
	 * @param maxCount
	 */
	public static void init(int poolCount, int initCount, int maxCount){
		init(poolCount, initCount, DEFAULT_INIT_DECR_FACTOR, maxCount, DEFAULT_MAX_DECR_FACTOR);
	}
	
	/**
	 * 初始化size
	 * @param poolSize
	 * @param initCount
	 * @param initDecrFactor
	 * @param maxCount
	 * @param maxDecrFactor
	 */
	public static void init(int poolCount, int initCount, int initDecrFactor, int maxCount, int maxDecrFactor){
		int maxCachedIndex = getPoolIndex(maxCachedBufferSize);
		for (int i=0; i<capacityArray.length; ++i){
			int initSize = 0;
			if (i <= maxCachedIndex){
				initSize = initCount;
			}
			ConcurrentObjectPool<CowBuffer> cpool = 
				new ConcurrentObjectPool<CowBuffer>(
					new CowBufferFactory(capacityArray[i]),
					poolCount, initSize, maxCount
				);
			cpoolArray.set(i, cpool);
			initCount /= initDecrFactor;
			maxCount /= maxDecrFactor;
		}
	}
	
	/**
	 * 从池中分配一个最小容量写时拷贝Buffer
	 * @return 不会为null
	 */
	public static CowBuffer borrowObject(){
		return cpoolArray.get(0).borrowObject();
	}
	
	/**
	 * 分配一个指定大小的写时拷贝Buffer
	 * @param requestedCapacity
	 * @param pool
	 * @return
	 */
	public static CowBuffer borrowObject(int requestedCapacity){
		if (requestedCapacity > maxCachedBufferSize){
			return new CowBuffer(requestedCapacity);
		}
		
		ConcurrentObjectPool<CowBuffer> cpool = getPool(requestedCapacity);
		if (cpool == null){
			return null;
		}
		return cpool.borrowObject();
	}
	
	public static ConcurrentObjectPool<CowBuffer> getPool(int requestedCapacity){
		if (requestedCapacity > maxCachedBufferSize){
			return null;
		}
		
		int index = getPoolIndex(requestedCapacity);
		if (index < 0){
			return null;
		}
		return cpoolArray.get(index);
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
