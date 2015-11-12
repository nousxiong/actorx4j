/**
 * 
 */
package actorx.util;

import actorx.Message;
import cque.INode;
import cque.NodePool;

/**
 * @author Xiong
 * 配对节点池
 */
public class MessagePairPool {
	private static final ThreadLocal<NodePool> pool;
	static{
		pool = new ThreadLocal<NodePool>();
	}
	private static int initSize = 0;
	private static int maxSize = Integer.MAX_VALUE;
	
	public static void init(int initSize, int maxSize){
		MessagePairPool.initSize = initSize;
		MessagePairPool.maxSize = maxSize;
	}
	
	public static final NodePool getNodePool(){
		NodePool p = pool.get();
		if (p == null){
			INode[] initNodes = null;
			if (MessagePairPool.initSize > 0){
				initNodes = new INode[MessagePairPool.initSize];
				for (int i=0; i<initNodes.length; ++i){
					initNodes[i] = new PairLinkedNode<Message>();
				}
			}
			p = new NodePool(initNodes, MessagePairPool.maxSize);
			pool.set(p);
		}
		return p;
	}
}
