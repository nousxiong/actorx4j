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
	
	public Message(int size){
		reserve(size);
	}
	
	public Message(ActorId sender){
		this.sender = sender;
	}
	
	public Message(ActorId sender, String type){
		this.sender = sender;
		this.type = type;
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
	
	public void set(int i, Object arg){
		args[i] = arg;
	}
	
	public void reserve(int size){
		if (args == null){
			args = new Object[size];
		}else{
			if (size <= args.length){
				return;
			}
			Object[] newArgs = new Object[size];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			args = newArgs;
		}
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
