/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * @creation 2016年9月15日下午12:08:18
 *
 */
public interface ISendFilter {
	/**
	 * 当发送指定类型的消息时调用
	 * @param toAid
	 * @param type
	 * @param prevMsg 由前一个过滤器过滤过的消息
	 * @param srcMsg
	 * @return
	 */
	Message filterSend(ActorId toAid, String type, Message prevMsg, Message srcMsg);
}
