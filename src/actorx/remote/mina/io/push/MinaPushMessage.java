/**
 * 
 */
package actorx.remote.mina.io.push;

import org.apache.mina.core.buffer.IoBuffer;

import actorx.remote.mina.io.AbstractMinaWrapMessage;
import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaPushMessage extends AbstractMinaWrapMessage {

	public MinaPushMessage(MinaMsgCode msgCode) {
		super(msgCode);
	}

	@Override
	protected void decode1(IoBuffer ioBuffer) {
		decodeBody(ioBuffer);
	}

	@Override
	protected void encode1(IoBuffer ioBuffer) {
		encodeBody(ioBuffer);
	}

}
