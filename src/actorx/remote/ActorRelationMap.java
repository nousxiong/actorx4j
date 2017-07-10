/**
 * 
 */
package actorx.remote;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import actorx.ActorId;

/**
 * @author Xiong
 *
 */
public class ActorRelationMap {
	// Key 发起链接的aid， Value 接收链接的aid
	private Map<ActorId, ActorId> relations = new HashMap<ActorId, ActorId>();
	
	public void addRelation(ActorId linkAid, ActorId linkedAid){
		relations.put(linkAid, linkedAid);
	}
	
	public boolean removeRelation(ActorId fromAid){
		return relations.remove(fromAid) != null;
	}
	
	public Iterator<Entry<ActorId, ActorId>> iterator(){
		return relations.entrySet().iterator();
	}
	
	public void clear(){
		relations.clear();
	}
}
