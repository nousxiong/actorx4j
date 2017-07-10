/**
 * 
 */
package actorx.util;

import actorx.ActorId;
import actorx.ActorSystem;

/**
 * @author Xiong
 *
 */
public final class ServiceUtils {
	public static ActorId makeServiceId(String axid, String name){
		return makeServiceId(Atom.to(axid), name);
	}
	
	public static ActorId makeServiceId(long axid, String name){
		return new ActorId(axid, 0, Atom.to(name));
	}
	
	public static ActorId makeServiceId(ActorSystem axSys, String name){
		return makeServiceId(axSys.getAxid(), name);
	}
}
