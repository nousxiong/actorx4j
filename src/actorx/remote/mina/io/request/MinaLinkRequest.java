/**
 * 
 */
package actorx.remote.mina.io.request;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaLinkRequest extends MinaRequestMessage {

	public MinaLinkRequest() {
		super(MinaMsgCode.LINK_REQ);
	}

}
