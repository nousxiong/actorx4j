/**
 * 
 */
package actorx.remote.mina.io.request;

import org.apache.mina.core.buffer.IoBuffer;

import actorx.remote.mina.io.AbstractMinaWrapMessage;
import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaRequestMessage extends AbstractMinaWrapMessage {

	public MinaRequestMessage(MinaMsgCode msgCode) {
		super(msgCode);
	}

	@Override
	protected final void decode1(IoBuffer ioBuffer) {
		decodeBody(ioBuffer);
	}

	@Override
	protected final void encode1(IoBuffer ioBuffer) {
		encodeBody(ioBuffer);
	}

}
