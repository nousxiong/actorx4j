/**
 * 
 */
package actorx.util;

/**
 * @author Xiong
 * 配对链表
 */
public class PairLinkedQueue<E> {
	private PairLinkedNode<E> head = null;
	private PairLinkedNode<E> tail = null;
	
	/**
	 * 在尾部添加一个元素
	 * @param 返回添加成功后的节点
	 * @return
	 */
	public PairLinkedNode<E> add(PairLinkedNode<E> newNode){
		if (this.tail == null){
			if (this.head == null){
				this.head = newNode;
				this.tail = newNode;
				newNode.prev = null;
				newNode.next = null;
				return newNode;
			}else{
				return insertBefore(this.head, newNode);
			}
		}else{
			return insertAfter(this.tail, newNode);
		}
	}
	
	/**
	 * 移除头部一个元素
	 * @return 返回移除的头部元素，如果返回null则说明队列空
	 */
	public PairLinkedNode<E> poll(){
		PairLinkedNode<E> node = head;
		if (head != null){
			node.pair = remove(head);
		}
		return node;
	}
	
	/**
	 * 移除一个node
	 * @param node
	 * @return 返回其pair
	 */
	public PairLinkedNode<E> remove(PairLinkedNode<E> node){
		PairLinkedNode<E> pair = node.pair;
		if (node.prev == null){
			this.head = node.next;
		}else{
			node.prev.next = node.next;
		}
		
		if (node.next == null){
			this.tail = node.prev;
		}else{
			node.next.prev  = node.prev;
		}
		
		// 防止循环引用
		node.pair = null;
		node.prev = null;
		node.next = null;
		return pair;
	}
	
	/**
	 * 是否为空
	 * @return
	 */
	public boolean isEmpty(){
		return head == null && tail == null;
	}
	
	/**
	 * 防止循环引用，清空队列
	 */
	public void clear(){
		while (!isEmpty()){
			PairLinkedNode<E> node = head;
			remove(head);
			node.release();
		}
		assert isEmpty();
	}
	
	private PairLinkedNode<E> insertAfter(PairLinkedNode<E> node, PairLinkedNode<E> newNode){
		newNode.prev = node;
		newNode.next = node.next;
		if (node.next == null){
			this.tail = newNode;
		}else{
			node.next.prev = newNode;
		}
		node.next = newNode;
		return newNode;
	}
	
	private PairLinkedNode<E> insertBefore(PairLinkedNode<E> node, PairLinkedNode<E> newNode){
		newNode.prev = node.prev;
		newNode.next = node;
		if (node.prev == null){
			this.head = newNode;
		}else{
			node.prev.next = newNode;
		}
		node.prev = newNode;
		return newNode;
	}
}
