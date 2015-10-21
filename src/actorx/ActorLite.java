/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * 简化版的Actor，不能接收消息，只能发送，有自己的ActorId
 */
public class ActorLite {
	private ActorId selfAid;
	
	public ActorLite(){
		this.selfAid = Context.getInstance().generateActorId();
	}
	
	public ActorId getActorId(){
		return selfAid;
	}
	
	public void send(ActorId aid){
		Actor.sendMessage(selfAid, aid, null);
	}
	
	public void send(ActorId aid, String type, Object... args){
		Actor.sendMessage(selfAid, aid, type, args);
	}
}
