/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.NodePool;
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
		NodePool pool = MessagePairPool.getNodePool();
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
		NodePool pool = MessagePairPool.getNodePool();
		assertTrue(pool.size() == 0);
		PairLinkedQueue<Message> leftList = new PairLinkedQueue<Message>();
		PairLinkedQueue<Message> rightList = new PairLinkedQueue<Message>();
		PairLinkedNode<Message> leftNode = leftList.add(getNode(new Message(null, "left name")));
		PairLinkedNode<Message> rightNode = rightList.add(getNode(new Message(null, "right name")));
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
