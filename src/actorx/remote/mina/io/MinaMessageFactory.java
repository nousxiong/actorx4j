/**
 * 
 */
package actorx.remote.mina.io;

import actorx.ICustomMessageFactory;
import actorx.remote.mina.io.push.*;
import actorx.remote.mina.io.request.*;
import actorx.remote.mina.io.response.*;

/**
 * @author Xiong
 *
 */
public class MinaMessageFactory {
	private java.util.concurrent.ConcurrentHashMap<String, ICustomMessageFactory> customMsgMap;
	
	public MinaMessageFactory(
		java.util.concurrent.ConcurrentHashMap<String, ICustomMessageFactory> customMsgMap, 
		int poolSize, int initSize, int maxSize
	){
		this.customMsgMap = customMsgMap;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends AbstractMinaMessage> T createInstance(MinaMsgCode msgCode) {
		AbstractMinaMessage msg = null;
		switch (msgCode){
		case HEART_REQ:
			msg = new MinaHeartRequest();
			break;
		case HEART_RESP:
			msg = new MinaHeartResponse();
			break;
		case LINK_REQ:
			msg = new MinaLinkRequest();
			break;
		case LINK_RESP:
			msg = new MinaLinkResponse();
			break;
		case MONITOR_REQ:
			msg = new MinaMonitorRequest();
			break;
		case MONITOR_RESP:
			msg = new MinaMonitorResponse();
			break;
		case EXIT_PUSH:
			msg = new MinaExitPush();
			break;
		case SEND_PUSH:
			msg = new MinaSendPush();
			break;
		default:
			break;
		}
		
		if (msg instanceof AbstractMinaWrapMessage){
			((AbstractMinaWrapMessage) msg).setCustomMessageMap(customMsgMap);
		}
		return (T) msg;
	}
}
