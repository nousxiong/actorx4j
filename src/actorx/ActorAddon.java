/**
 * 
 */
package actorx;

import co.paralleluniverse.fibers.Suspendable;

/**
 * @author Xiong
 * @creation 2016年10月9日下午3:04:06
 *
 */
public class ActorAddon extends ActorRef {

	public ActorAddon(Actor hostAx) {
		super(hostAx.getActorSystem(), hostAx.getActorId());
	}

	@Suspendable
	@Override
	public void send(){
		super.send();
	}

	@Suspendable
	@Override
	public void send(String type){
		super.send(type);
	}

	@Suspendable
	@Override
	public <A> void send(String type, A arg){
		super.send(type, arg);
	}

	@Suspendable
	@Override
	public <A, A1> void send(String type, A arg, A1 arg1){
		super.send(type, arg, arg1);
	}

	@Suspendable
	@Override
	public <A, A1, A2> void send(String type, A arg, A1 arg1, A2 arg2){
		super.send(type, arg, arg1, arg2);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3){
		super.send(type, arg, arg1, arg2, arg3);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4){
		super.send(type, arg, arg1, arg2, arg3, arg4);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4, A5> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5){
		super.send(type, arg, arg1, arg2, arg3, arg4, arg5);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4, A5, A6> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6){
		super.send(type, arg, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4, A5, A6, A7> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7){
		super.send(type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4, A5, A6, A7, A8> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8){
		super.send(type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
	}

	@Suspendable
	@Override
	public <A, A1, A2, A3, A4, A5, A6, A7, A8, A9> void send(String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8, A9 arg9){
		super.send(type, arg, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
	}

	@Suspendable
	@Override
	public <C extends Message> void csend(C msg){
		super.csend(msg);
	}

	@Suspendable
	@Override
	public <C extends Message> void csend(String type, C msg){
		super.csend(type, msg);
	}

//	@Suspendable
//	@Override
//	public void send(String type, Object... args){
//		super.send(type, args);
//	}
}
