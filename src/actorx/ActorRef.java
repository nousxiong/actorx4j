/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * @creation 2016年10月9日下午2:33:51
 *
 */
public class ActorRef {

	public ActorRef(AxSystem axs, ActorId refAid){
		this.axs = axs;
		this.refAid = refAid;
	}
	
	public AxSystem getAxSystem(){
		return axs;
	}
	
	public ActorId getRefAid(){
		return refAid;
	}
	
	public void send(){
		send(ActorId.NULL);
	}
	
	public void send(ActorId fromAid){
		axs.send(fromAid, refAid);
	}
	
	public <A> void send(String type, A arg){
		send(ActorId.NULL, type, arg);
	}
	
	public <A> void send(ActorId fromAid, String type, A arg){
		axs.send(fromAid, refAid, type, arg);
	}
	
	public <A1, A2> void send(String type, A1 arg1, A2 arg2){
		send(ActorId.NULL, type, arg1, arg2);
	}
	
	public <A1, A2> void send(ActorId fromAid, String type, A1 arg1, A2 arg2){
		axs.send(fromAid, refAid, type, arg1, arg2);
	}
	
	public <A1, A2, A3> void send(String type, A1 arg1, A2 arg2, A3 arg3){
		send(ActorId.NULL, type, arg1, arg2, arg3);
	}
	
	public <A1, A2, A3> void send(ActorId fromAid, String type, A1 arg1, A2 arg2, A3 arg3){
		axs.send(fromAid, refAid, type, arg1, arg2, arg3);
	}
	
	public void send(String type, Object... args){
		send(ActorId.NULL, type, args);
	}
	
	public void send(ActorId fromAid, String type, Object... args){
		axs.send(fromAid, refAid, type, args);
	}
	
	private AxSystem axs;
	private ActorId refAid;
}
