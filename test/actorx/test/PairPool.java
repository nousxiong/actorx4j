/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.INodePool;
import actorx.Message;
import actorx.util.MessagePairPool;
import actorx.util.PairLinkedNode;
import actorx.util.PairLinkedQueue;

/**
 * @author Xiong
 *
 */
public class PairPool{
	
	private PairLinkedNode<Message> getNode(Message msg){
		INodePool pool = MessagePairPool.getNodePool();
		PairLinkedNode<Message> n = pool.get();
		if (n == null){
			n = new PairLinkedNode<Message>();
			n.onGet(pool);
		}
		n.data = msg;
		return n;
	}
	
	@Test
	public void test(){
		INodePool pool = MessagePairPool.getNodePool();
		assertTrue(pool.size() == 0);
		PairLinkedQueue<Message> leftList = new PairLinkedQueue<Message>();
		PairLinkedQueue<Message> rightList = new PairLinkedQueue<Message>();
		Message msg1 = new Message();
		msg1.setType("left name");
		Message msg2 = new Message();
		msg2.setType("right name");
		PairLinkedNode<Message> leftNode = leftList.add(getNode(msg1));
		PairLinkedNode<Message> rightNode = rightList.add(getNode(msg2));
		assertTrue(pool.size() == 0);
		
		leftNode.pair = rightNode;
		rightNode.pair = leftNode;
		
		PairLinkedNode<Message> pair = leftNode.pair;
		leftList.remove(leftNode);
		assertTrue(leftList.isEmpty());
		assertTrue(pair == rightNode);
		
		leftNode.release();
		assertTrue(pool.size() == 1);
		
		rightList.remove(pair);
		assertTrue(rightList.isEmpty());
		
		pair.release();
		assertTrue(pool.size() == 2);
	}

}
