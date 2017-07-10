/**
 * 
 */
package actorx.remote.mina.io.response;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaLinkResponse extends MinaRelationResponse {

	public MinaLinkResponse() {
		super(MinaMsgCode.LINK_RESP);
	}

}
