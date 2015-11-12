/**
 * 
 */
package actorx.util;

import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 * 配对链表节点
 */
public class PairLinkedNode<E> implements INode {
	public E data;
	public PairLinkedNode<E> prev = null;
	public PairLinkedNode<E> next = null;
	public PairLinkedNode<E> pair = null;
	
	public PairLinkedNode(){
	}
	
	public PairLinkedNode(E e){
		this.data = e;
	}
	
	/** 以下实现了INode */
	private INode poolNext;
	private IFreer freer;

	@Override
	public INode getNext(){
		return poolNext;
	}

	@Override
	public void onFree(){
		poolNext = null;
		freer = null;
		data = null;
		pair = null;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.poolNext = null;
	}

	@Override
	public void release(){
		if (freer != null){
			freer.free(this);
		}
	}

	@Override
	public void setNext(INode next){
		this.poolNext = next;
	}
}
