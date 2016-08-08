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
		setType(et);
		setErrmsg(errmsg);
	}
	
	public void setType(ExitType et){
		this.type = et.getValue();
	}
	
	public void setErrmsg(String errmsg){
		this.errmsg = errmsg;
	}
	
	public ExitType getType(){
		return ExitType.parse(type);
	}
	
	public String getErrmsg(){
		return errmsg;
	}
}
