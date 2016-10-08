/**
 * 
 */
package actorx;

import java.util.concurrent.ConcurrentLinkedQueue;

import actorx.detail.MessageFactory;
import cque.ConcurrentNodePool;
import cque.INode;
import cque.MpscNodePool;

/**
 * @author Xiong
 * 消息对象池
 */
public class MessagePool {
	
	/** 消息节点池 */
	private static final ConcurrentNodePool<Message> cpool = 
		new ConcurrentNodePool<Message>(new MessageFactory());
	/** 消息初始列表队列 */
	private static final ConcurrentLinkedQueue<INode[]> initListQue = 
		new ConcurrentLinkedQueue<INode[]>();
	
	/**
	 * 初始化size
	 * @param initSize
	 * @param maxSize
	 */
	public static void init(int initSize, int maxSize){
		init(initSize, maxSize, 0);
	}
	
	/**
	 * 初始化size并预先创建
	 * @param initSize
	 * @param maxSize
	 * @param createGroupSize 创建多少组initSize+maxSize
	 */
	public static void init(int initSize, int maxSize, int createGroupSize){
		assert cpool != null;
		cpool.setInitSize(initSize);
		cpool.setMaxSize(maxSize);
		
		MessageFactory msgFactory = new MessageFactory();
		for (int i=0; i<createGroupSize; ++i){
			INode[] initList = new INode[initSize];
			for (int n=0; n<initList.length; ++n){
				initList[n] = msgFactory.createInstance();
			}
			initListQue.add(initList);
		}
	}
	
	/**
	 * 尝试获取一个初始化列表
	 * @return
	 */
	public static INode[] fetchInitList(){
		return initListQue.poll();
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
	
	/**
	 * 取得本地线程的池
	 * @param initList
	 * @return
	 */
	public static MpscNodePool<Message> getLocalPool(INode[] initList){
		return cpool.getLocalPool(initList);
	}
	
	/**
	 * 初始化本线程池
	 */
	public static void initLocalPool(){
		cpool.getLocalPool(fetchInitList());
	}
}
