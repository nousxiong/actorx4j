/**
 * 
 */
package actorx.remote.mina.io.push;

import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaExitPush extends MinaPushMessage {

	public MinaExitPush() {
		super(MinaMsgCode.EXIT_PUSH);
	}

}
