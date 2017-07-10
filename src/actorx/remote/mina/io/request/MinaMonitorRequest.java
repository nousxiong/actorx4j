/**
 * 
 */
package actorx.remote.mina.io.request;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaMonitorRequest extends MinaRequestMessage {

	public MinaMonitorRequest() {
		super(MinaMsgCode.MONITOR_REQ);
	}

}
