/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.util.PairLinkedNode;

/**
 * @author Xiong
 * 双向链表+配对节点
 */
public class PairLinked {
	class Data{
		@SuppressWarnings("unused")
		private int id = 0;
		@SuppressWarnings("unused")
		private String name = "uname";
		@SuppressWarnings("unused")
		private Object[] args;
		
		public Data(int id, String name, Object... args){
			this.id = id;
			this.name = name;
			this.args = args;
		}
	}
	
	@Test
	public void test() {
		actorx.util.PairLinkedQueue<Data> leftList = new actorx.util.PairLinkedQueue<Data>();
		actorx.util.PairLinkedQueue<Data> rightList = new actorx.util.PairLinkedQueue<Data>();
		actorx.util.PairLinkedNode<Data> leftNode = leftList.add(new PairLinkedNode<Data>(new Data(0, "left name", 20, "string")));
		actorx.util.PairLinkedNode<Data> rightNode = rightList.add(new PairLinkedNode<Data>(new Data(0, "right name", 20, "string")));
		
		leftNode.pair = rightNode;
		rightNode.pair = leftNode;
		
		actorx.util.PairLinkedNode<Data> pair = leftNode.pair;
		leftList.remove(leftNode);
		assertTrue(leftList.isEmpty());
		assertTrue(pair == rightNode);
		rightList.remove(pair);
		assertTrue(rightList.isEmpty());
	}

}
