/**
 * 
 */
package actorx.remote;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.ActorId;
import actorx.Message;

/**
 * @author Xiong
 *
 */
public interface INetworkClientService extends INetworkService {
	void sendRemote(ActorId toAid, Message smsg) throws SuspendExecution, Exception;
}
