/**
 * 
 */
package actorx.remote;

import co.paralleluniverse.fibers.SuspendExecution;
import actorx.Message;

/**
 * @author Xiong
 *
 */
public interface INetworkService {
	void start() throws SuspendExecution, Exception;
	void handleMessage(Message msg) throws SuspendExecution, Exception;
	void handleException(Throwable e) throws SuspendExecution;
	void stop() throws SuspendExecution;
}
