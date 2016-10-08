/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * @creation 2016年9月15日下午5:56:01
 *
 */
public interface IRecvFilter {
	/**
	 * 当收到指定类型的消息时调用
	 * @param fromAid
	 * @param type
	 * @param prevMsg 由前一个过滤器过滤过的消息
	 * @param srcMsg
	 * @return
	 */
	Message filterRecv(ActorId fromAid, String type, Message prevMsg, Message srcMsg);
}
