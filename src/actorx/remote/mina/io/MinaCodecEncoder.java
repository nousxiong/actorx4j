package actorx.remote.mina.io;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Xiong
 */
class MinaCodecEncoder extends ProtocolEncoderAdapter {

	@Override
	public final void encode(IoSession session, Object obj, ProtocolEncoderOutput out) throws Exception {
		IMinaMessage msg = (IMinaMessage) obj;
		IoBuffer ioBuffer = msg.encode();
		out.write(ioBuffer);
	}

}
