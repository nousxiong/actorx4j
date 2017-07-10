/**
 * 
 */
package actorx.remote;

import actorx.ActorId;

/**
 * @author Xiong
 *
 */
public class NetSession {
	private ActorId handleAid;
	
	public ActorId getHandleAid() {
		return handleAid;
	}
	
	public void setHandleAid(ActorId handleAid) {
		this.handleAid = handleAid;
	}

	public void close() {}
}
