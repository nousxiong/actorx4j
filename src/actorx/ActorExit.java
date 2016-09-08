/**
 * 
 */
package actorx;

/**
 * @author Xiong
 *
 */
public class ActorExit extends actorx.adl.ActorExit {
	
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
}
