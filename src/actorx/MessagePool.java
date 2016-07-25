/**
 * 
 */
package actorx;

import actorx.util.MessageFactory;
import cque.ConcurrentNodePool;
import cque.MpscNodePool;

/**
 * @author Xiong
 * 消息对象池
 */
public class MessagePool {
	
	/** 消息节点池 */
	private static final ConcurrentNodePool<Message> cpool = 
		new ConcurrentNodePool<Message>(new MessageFactory());
	
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
	 * 从池中分配一个消息
	 * @return 不会为null
	 */
	public static Message get(){
		MpscNodePool<Message> pool = getLocalPool();
		return get(pool);
	}
	
	/**
	 * 使用用户之前缓存的池来分配一个消息
	 * @param pool
	 * @return 不会为null
	 */
	public static Message get(MpscNodePool<Message> pool){
		assert pool == getLocalPool();
		return cpool.get(pool);
	}
	
	/**
	 * 取得本地线程的池
	 * @return
	 */
	public static MpscNodePool<Message> getLocalPool(){
		return cpool.getLocalPool();
	}
}
