/**
 * 
 */
package actorx;

import actorx.detail.MessageFactory;
import cque.ConcurrentObjectPool;

/**
 * @author Xiong
 * 消息对象池
 */
public class MessagePool {
	/** 消息节点池 */
	private static ConcurrentObjectPool<Message> cpool = 
		new ConcurrentObjectPool<Message>(new MessageFactory());
	
	/**
	 * 初始化size
	 * @param initSize
	 * @param maxSize
	 */
	public static void init(int poolSize, int initSize, int maxSize){
		cpool = new ConcurrentObjectPool<Message>(new MessageFactory(), poolSize, initSize, maxSize);
	}
	
	/**
	 * 从池中分配一个消息
	 * @return 不会为null
	 */
	public static Message borrowObject(){
		return cpool.borrowObject();
	}
}
