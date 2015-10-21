/**
 * 
 */
package actorx;

/**
 * @author Xiong
 */
public class Message {
	private ActorId sender;
	private String type;
	private Object[] args;
	
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
	
	public String getType(){
		return type;
	}
	
	public <T> T get(Class<T> c, int i){
		if (i >= args.length){
			return null;
		}
		
		return c.cast(args[i]);
	}
	
	public Object[] get(){
		return args;
	}
}
