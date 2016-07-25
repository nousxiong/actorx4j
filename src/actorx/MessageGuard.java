/**
 * 
 */
package actorx;

import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 * 消息守护者
 */
public class MessageGuard implements AutoCloseable, INode {
	private Message msg = null;

	public MessageGuard wrap(Message msg){
		this.msg = msg;
		return this;
	}
	
	public Message get(){
		return msg;
	}
	
	@Override
	public void close() throws Exception {
		msg.release();
		msg = null;
		release();
	}

	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;

	@Override
	public void release(){
		if (freer != null){
			freer.free(this);
			freer = null;
		}
	}

	@Override
	public INode getNext(){
		return next;
	}

	@Override
	public INode fetchNext(){
		INode nx = next;
		next = null;
		return nx;
	}

	@Override
	public void onFree(){
		next = null;
		freer = null;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.next = null;
	}

	@Override
	public void setNext(INode next){
		this.next = next;
	}
}
