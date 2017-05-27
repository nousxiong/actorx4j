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
		send(ActorId.NULLAID);
	}

	@Suspendable
	public void send(ActorId fromAid){
		axs.send(fromAid, refAid);
	}

	@Suspendable
	public void send(String type){
		axs.send(ActorId.NULLAID, refAid, type);
	}

	@Suspendable
	public <A> void send(String type, A arg){
		send(ActorId.NULLAID, type, arg);
	}

	@Suspendable
	public <A> void send(ActorId fromAid, String type, A arg){
		axs.send(fromAid, refAid, type, arg);
	}

	@Suspendable
	public <A, A1> void send(String type, A arg, A1 arg1){
		send(ActorId.NULLAID, type, arg, arg1);
	}

	@Suspendable
	public <A, A1> void send(ActorId fromAid, String type, A arg, A1 arg1){
		axs.send(fromAid, refAid, type, arg, arg1);
	}

	@Suspendable
	public <A, A1, A2> void send(String type, A arg, A1 arg1, A2 arg2){
		send(ActorId.NULLAID, type, arg, arg1, arg2);
	}

	@Suspendable
	public <A, A1, A2> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2){
		axs.send(fromAid, refAid, type, arg, arg1, arg2);
	}

	@Suspendable
	public <A, A1, A2, A3> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3);
	}

	@Suspendable
	public <A, A1, A2, A3> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3);
	}

	@Suspendable
	public <A, A1, A2, A3, A4> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4);
	}

	@Suspendable
	public <A, A1, A2, A3, A4> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4, arg5);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4, arg5);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8, A9> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8, A9 arg9){
		send(ActorId.NULLAID, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
	}

	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8, A9> void send(ActorId fromAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8, A9 arg9){
		axs.send(fromAid, refAid, type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
	}

	@Suspendable
	public <C extends Message> void csend(C msg){
		csend(ActorId.NULLAID, msg);
	}

	@Suspendable
	public <C extends Message> void csend(ActorId fromAid, C msg){
		msg.setSender(fromAid);
		axs.send(refAid, msg);
	}

	@Suspendable
	public <C extends Message> void csend(String type, C msg){
		csend(ActorId.NULLAID, type, msg);
	}

	@Suspendable
	public <C extends Message> void csend(ActorId fromAid, String type, C msg){
		msg.setSender(fromAid);
		msg.setType(type);
		axs.send(refAid, msg);
	}

//	@Suspendable
//	public void send(String type, Object... args){
//		send(ActorId.NULLAID, type, args);
//	}
//
//	@Suspendable
//	public void send(ActorId fromAid, String type, Object... args){
//		axs.send(fromAid, refAid, type, args);
//	}
	
	private ActorSystem axs;
	private ActorId refAid;
}
