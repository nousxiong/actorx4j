/**
 * 
 */
package actorx.util;

/**
 * @author Xiong
 * 配对链表节点
 */
public class PairLinkedNode<E> {
	public E data;
	public PairLinkedNode<E> prev = null;
	public PairLinkedNode<E> next = null;
	public PairLinkedNode<E> pair = null;
	
	public PairLinkedNode(E e){
		this.data = e;
	}
}
