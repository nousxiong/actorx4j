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
	public void send(String type, Object... args){
		super.send(type, args);
	}
}
