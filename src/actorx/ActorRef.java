/**
 * 
 */
package actorx;

import co.paralleluniverse.fibers.Suspendable;

/**
 * @author Xiong
 * @creation 2016年10月9日下午2:33:51
 *
 */
public class ActorRef {

	public ActorRef(ActorSystem axs, ActorId refAid){
		this.axs = axs;
		this.refAid = refAid;
	}
	
	public ActorSystem getAxSystem(){
		return axs;
	}
	
	public ActorId getRefAid(){
		return refAid;
	}

	@Suspendable
	public void send(){
		send(ActorId.NULL);
	}

	@Suspendable
	public void send(ActorId fromAid){
		axs.send(fromAid, refAid);
	}

	@Suspendable
	public void send(String type){
		axs.send(ActorId.NULL, refAid, type);
	}

	@Suspendable
	public <A> void send(String type, A arg){
		send(ActorId.NULL, type, arg);
	}

	@Suspendable
	public <A> void send(ActorId fromAid, String type, A arg){
		axs.send(fromAid, refAid, type, arg);
	}

	@Suspendable
	public <A, A1> void send(String type, A arg, A1 arg1){
		send(ActorId.NULL, type, arg, arg1);
	}

	@Suspendable
	public <A, A1> void send(ActorId fromAid, String type, A arg, A1 arg1){
		axs.send(fromAid, refAid, type, arg, arg1);
	}

	@Suspendable
	public <A, A1, A2> void send(String type, A arg, A1 arg1, A2 arg2){
		send(ActorId.NULL, type, arg, arg1, arg2);
	}

	@Suspendable
	public <A, A1, A2> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2){
		axs.send(fromAid, refAid, type, arg, arg1, arg2);
	}

	@Suspendable
	public void send(String type, Object... args){
		send(ActorId.NULL, type, args);
	}

	@Suspendable
	public void send(ActorId fromAid, String type, Object... args){
		axs.send(fromAid, refAid, type, args);
	}
	
	private ActorSystem axs;
	private ActorId refAid;
}
