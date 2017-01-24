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
		selfAid = Context.getInstance().generateActorId();
	}
	
	public ActorId getActorId(){
		return selfAid;
	}
	
	public void send(ActorId aid){
		Actor.sendMessage(selfAid, aid);
	}
	
	public void send(ActorId aid, Message msg){
		Actor.sendMessage(selfAid, aid, msg);
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type
	 * @param args
	 */
	public void send(ActorId aid, String type, Object... args){
		Actor.sendMessage(selfAid, aid, type, args);
	}
	
	public void send(ActorId aid, String type){
		Actor.sendMessage(selfAid, aid, type);
	}
	
	public void send(ActorId aid, String type, Object arg){
		Actor.sendMessage(selfAid, aid, type, arg);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1){
		Actor.sendMessage(selfAid, aid, type, arg, arg1);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1, Object arg2){
		Actor.sendMessage(selfAid, aid, type, arg, arg1, arg2);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1, Object arg2, Object arg3){
		Actor.sendMessage(selfAid, aid, type, arg, arg1, arg2, arg3);
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param msg
	 * @param type
	 * @param args
	 */
	public void send(ActorId aid, Message msg, String type, Object... args){
		Actor.sendMessage(selfAid, aid, msg, type, args);
	}
	
	public void send(ActorId aid, Message msg, String type){
		Actor.sendMessage(selfAid, aid, msg, type);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg){
		Actor.sendMessage(selfAid, aid, msg, type, arg);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1){
		Actor.sendMessage(selfAid, aid, msg, type, arg, arg1);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1, Object arg2){
		Actor.sendMessage(selfAid, aid, msg, type, arg, arg1, arg2);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1, Object arg2, Object arg3){
		Actor.sendMessage(selfAid, aid, msg, type, arg, arg1, arg2, arg3);
	}
}
