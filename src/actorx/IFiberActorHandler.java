/**
 * 
 */
package actorx;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * @author Xiong
 * @creation 2017年3月26日上午12:22:06
 *
 */
public interface IFiberActorHandler {
	void run(Actor self) throws SuspendExecution, Exception;
}
