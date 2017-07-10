/**
 * 
 */
package actorx.remote.mina;

import java.util.Iterator;
import java.util.Map.Entry;

import co.paralleluniverse.fibers.SuspendExecution;
import cque.ConcurrentObjectPool;
import cque.Node;
import actorx.Actor;
import actorx.ActorAddon;
import actorx.ActorExit;
import actorx.ActorId;
import actorx.ActorSystem;
import actorx.AtomCode;
import actorx.ExitType;
import actorx.remote.ActorRelationMap;
import actorx.remote.INetworkService;

/**
 * @author Xiong
 *
 */
public abstract class AbstractMinaService extends ActorAddon implements INetworkService {
	protected Actor hostAx;
	protected ActorSystem axSys;
	protected ConcurrentObjectPool<Node<Object>> cpool;

	protected ActorRelationMap relations;
//	protected ActorRelationMap monitorMap;
	
	protected AbstractMinaService(
		Actor hostAx, 
		ConcurrentObjectPool<Node<Object>> cpool,
		ActorRelationMap relations
//		ActorRelationMap monitorMap
	){
		super(hostAx);
		this.hostAx = hostAx;
		this.axSys = hostAx.getActorSystem();
		this.cpool = cpool;
		this.relations = relations;
	}
	
	protected void sendNetErrorExits(String errmsg) throws SuspendExecution {
		// 对已经链接的发送退出消息
		Iterator<Entry<ActorId, ActorId>> linkItr = relations.iterator();
		while (linkItr.hasNext()){
			Entry<ActorId, ActorId> entry = linkItr.next();
			ActorId localAid = null;
			ActorId remoteAid = null;
			if (axSys.isLocalActor(entry.getKey())){
				localAid = entry.getKey();
				remoteAid = entry.getValue();
				relations.removeRelation(remoteAid);
			}else if (axSys.isLocalActor(entry.getValue())){
				localAid = entry.getValue();
				remoteAid = entry.getKey();
				relations.removeRelation(remoteAid);
			}
			
			sendNetErrorExit(remoteAid, localAid, errmsg);
		}
		relations.clear();

//		Iterator<Entry<ActorId, ActorId>> monitorItr = monitorMap.iterator();
//		while (monitorItr.hasNext()){
//			Entry<ActorId, ActorId> entry = monitorItr.next();
//			ActorId localAid = null;
//			ActorId remoteAid = null;
//			if (axSys.isLocalActor(entry.getKey())){
//				localAid = entry.getKey();
//				remoteAid = entry.getValue();
//				monitorMap.removeRelation(remoteAid);
//				sendNetErrorExit(remoteAid, localAid, errmsg);
//			}
//		}
//		monitorMap.clear();
	}
	
	protected void sendNetErrorExit(ActorId fromAid, ActorId toAid, String errmsg) throws SuspendExecution {
		ActorExit axExit = new ActorExit();
		axExit.setExitType(ExitType.NETERR);
		axExit.setErrmsg(errmsg);
		axSys.send(fromAid, toAid, AtomCode.EXIT, axExit);
	}
	
	protected void actorExit(ActorId fromAid, ActorId toAid){
		relations.removeRelation(fromAid);
		relations.removeRelation(toAid);
	}
}
