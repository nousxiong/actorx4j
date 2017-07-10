/**
 * 
 */
package actorx.remote.mina.io.response;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaMonitorResponse extends MinaRelationResponse {

	public MinaMonitorResponse() {
		super(MinaMsgCode.MONITOR_RESP);
	}

}
