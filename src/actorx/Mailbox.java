/**
 * 
 */
package actorx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import actorx.util.PairLinkedNode;
import actorx.util.PairLinkedQueue;

/**
 * @author Xiong
 * actor的邮箱
 */
public class Mailbox {
	private PairLinkedQueue<Message> totalList = new PairLinkedQueue<Message>();
	private Map<String, PairLinkedQueue<Message>> typedMap = new HashMap<String, PairLinkedQueue<Message>>();
	
	public void add(Message msg){
		PairLinkedNode<Message> totalNode = totalList.add(msg);
		String type = msg.getType();
		if (type != null){
			PairLinkedQueue<Message> typeList = typedMap.get(type);
			if (typeList == null){
				typeList = new PairLinkedQueue<Message>();
				typedMap.put(type, typeList);
			}
			PairLinkedNode<Message> typeNode = typeList.add(msg);
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
					assert(typeList != null);
					assert(totalNode.pair != null);
					
					// 使用totalNode.pair来移除配对
					PairLinkedNode<Message> pair = typeList.remove(totalNode.pair);
					assert(pair == totalNode);
				}
			}
		}else{
			for (String type : matchedTypes){
				PairLinkedQueue<Message> typeList = typedMap.get(type);
				if (typeList != null){
					PairLinkedNode<Message> typeNode = typeList.poll();
					if (typeNode != null){
						msg = typeNode.data;
						assert(typeNode.pair != null);
						
						PairLinkedNode<Message> pair = totalList.remove(typeNode.pair);
						assert(pair == typeNode);
						break;
					}
				}
			}
		}
		return msg;
	}
	
	public void clear(){
		totalList.clear();
		typedMap.clear();
	}
	
	private static void makePair(PairLinkedNode<Message> left, PairLinkedNode<Message> right){
		assert(left != right);
		left.pair = right;
		right.pair = left;
	} 
}
