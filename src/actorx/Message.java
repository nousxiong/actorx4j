/**
 * 
 */
package actorx;

import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 */
public class Message implements INode {
	private ActorId sender;
	private String type;
	private Object[] args;
	
	public Message(){
	}
	
	public Message(ActorId sender, Object... args){
		this.sender = sender;
		this.type = null;
		this.args = args;
	}
	
	public Message(ActorId sender, String type, Object... args){
		this.sender = sender;
		this.type = type;
		this.args = args;
	}
	
	public ActorId getSender(){
		return sender;
	}
	
	public void setSender(ActorId sender){
		this.sender = sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(int i){
		if (i >= args.length){
			return null;
		}
		
		return (T) args[i];
	}
	
	public Object[] get(){
		return args;
	}
	
	public void set(Object... args){
		this.args = args;
	}
	
	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;

	@Override
	public void release(){
		if (freer != null){
			freer.free(this);
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
		sender = null;
		type = null;
		args = null;
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
