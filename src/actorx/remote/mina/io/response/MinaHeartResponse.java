/**
 * 
 */
package actorx.remote.mina.io.response;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaHeartResponse extends MinaResponseMessage {

	public MinaHeartResponse() {
		super(MinaMsgCode.HEART_RESP);
	}

}
