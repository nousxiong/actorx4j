/**
 * 
 */
package actorx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cque.INodePool;
import actorx.util.MessagePairPool;
import actorx.util.PairLinkedNode;
import actorx.util.PairLinkedQueue;

/**
 * @author Xiong
 * actor的邮箱
 */
public class Mailbox {
	private PairLinkedQueue<Message> totalList = new PairLinkedQueue<Message>();
	private Map<String, PairLinkedQueue<Message>> typedMap = new HashMap<String, PairLinkedQueue<Message>>();
	private INodePool pool;
	
	public void add(Message msg){
		PairLinkedNode<Message> totalNode = totalList.add(getNode(msg));
		String type = msg.getType();
		if (type != null){
			PairLinkedQueue<Message> typeList = typedMap.get(type);
			if (typeList == null){
				typeList = new PairLinkedQueue<Message>();
				typedMap.put(type, typeList);
			}
			PairLinkedNode<Message> typeNode = typeList.add(getNode(msg));
			makePair(totalNode, typeNode);
		}
	}
	
	public Message fetch(List<String> matchedTypes){
		Message msg = null;
		if (matchedTypes.isEmpty()){
			PairLinkedNode<Message> totalNode = totalList.poll();
			if (totalNode != null){
				msg = totalNode.data;
				String type = msg.getType();
				if (type != null){
					PairLinkedQueue<Message> typeList = typedMap.get(type);
					assert typeList != null;
					assert totalNode.pair != null;
					
					// 使用totalNode.pair来移除配对
					PairLinkedNode<Message> pair = typeList.remove(totalNode.pair);
					assert pair == totalNode;
					totalNode.pair.release();
				}
				totalNode.release();
			}
		}else{
			for (String type : matchedTypes){
				PairLinkedQueue<Message> typeList = typedMap.get(type);
				if (typeList != null){
					PairLinkedNode<Message> typeNode = typeList.poll();
					if (typeNode != null){
						msg = typeNode.data;
						assert typeNode.pair != null;
						
						PairLinkedNode<Message> pair = totalList.remove(typeNode.pair);
						assert pair == typeNode;
						typeNode.pair.release();
						typeNode.release();
						break;
					}
				}
			}
		}
		return msg;
	}
	
	public void clear(){
		totalList.clear();
		for (PairLinkedQueue<Message> que : typedMap.values()){
			que.clear();
		}
		typedMap.clear();
	}
	
	private PairLinkedNode<Message> getNode(Message msg){
		INodePool pool = getNodePool();
		PairLinkedNode<Message> n = pool.get();
		if (n == null){
			n = new PairLinkedNode<Message>();
			n.onGet(pool);
		}
		n.data = msg;
		return n;
	}
	
	private INodePool getNodePool(){
		if (pool == null){
			pool = MessagePairPool.getNodePool();
		}
		return pool;
	}
	
	private static void makePair(PairLinkedNode<Message> left, PairLinkedNode<Message> right){
		assert left != right;
		left.pair = right;
		right.pair = left;
	} 
}
