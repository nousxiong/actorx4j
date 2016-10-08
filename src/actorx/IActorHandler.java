/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * 基于线程的actor可执行体
 */
public interface IActorHandler {
	void run(Actor self) throws Exception;
}
