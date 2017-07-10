/**
 * 
 */
package actorx.remote.mina.io.push;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaSendPush extends MinaPushMessage {

	public MinaSendPush() {
		super(MinaMsgCode.SEND_PUSH);
	}

}
