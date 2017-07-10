/**
 * 
 */
package actorx.remote.mina.io.request;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaHeartRequest extends MinaRequestMessage {

	public MinaHeartRequest() {
		super(MinaMsgCode.HEART_REQ);
	}
}
