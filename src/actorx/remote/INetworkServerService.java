/**
 * 
 */
package actorx.remote;

import actorx.ActorId;
import actorx.Message;
import co.paralleluniverse.fibers.SuspendExecution;

/**
 * @author Xiong
 *
 */
public interface INetworkServerService extends INetworkService {
	void sendRemote(ActorId toAid, Message smsg, NetSession netSession) throws SuspendExecution, Exception;
}
