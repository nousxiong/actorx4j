/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * @creation 2016年10月9日下午3:04:06
 *
 */
public class Addon extends ActorRef {

	public Addon(Actor hostAx) {
		super(hostAx.getAxSystem(), hostAx.getActorId());
	}

	@Override
	public void send(){
		super.send();
	}
	
	@Override
	public <A> void send(String type, A arg){
		super.send(type, arg);
	}
	
	@Override
	public <A, A1> void send(String type, A arg, A1 arg1){
		super.send(type, arg, arg1);
	}
	
	@Override
	public <A, A1, A2> void send(String type, A arg, A1 arg1, A2 arg2){
		super.send(type, arg, arg1, arg2);
	}
	
	@Override
	public void send(String type, Object... args){
		super.send(type, args);
	}
}
