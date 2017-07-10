/**
 * 
 */
package actorx;

import actorx.adl.IAdlAdapter;

/**
 * @author Xiong
 *
 */
public class ActorExit extends actorx.adl.ActorExit implements IAdlAdapter {
	// 不参与序列化，仅仅是夹带用途
	private ActorId sender;
	
	public ActorExit(){
		this(ExitType.NORMAL, "no error");
	}
	
	public ActorExit(ExitType et, String errmsg){
		setExitType(et);
		setErrmsg(errmsg);
	}
	
	public void setExitType(ExitType et){
		this.type = et.getValue();
	}
	
	public ExitType getExitType(){
		return ExitType.parse(type);
	}

	public ActorId getSender() {
		return sender;
	}

	public void setSender(ActorId sender) {
		this.sender = sender;
	}
}
