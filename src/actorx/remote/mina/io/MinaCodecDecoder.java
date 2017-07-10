/**
 * 
 */
package actorx.remote.mina.io;

import amina.codec.PooledCumulativeProtocolDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * @author Xiong
 * @creation 2017年2月10日下午6:58:40
 *
 */
public class MinaCodecDecoder extends PooledCumulativeProtocolDecoder {
	private static final int PACKAGE_LIMIT = 16 * 1024;
	private MinaMessageFactory messageFactory;
	
	public MinaCodecDecoder(MinaMessageFactory messageFactory){
		this.messageFactory = messageFactory;
	}

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		// 满足能够读取长度
		if (in.remaining() < IMinaMessage.HEADER_LENGTH){
			return false;
		}
		
		// 标记当前位置
		in.mark();
		
		// 长度和消息码
		int length = in.getUnsignedShort();
		int code = in.getUnsignedShort();
		
		// 检查消息码
		MinaMsgCode msgCode = MinaMsgCode.parse(code);
		
		// 检查长度
		if (length > PACKAGE_LIMIT) {
			throw new RuntimeException(
				new StringBuilder()
					.append("Session ").append(session.getId())
					.append(" msgCode ").append(msgCode)
					.append(" beyond ").append(PACKAGE_LIMIT).toString()
			);
		}
		
		if (in.remaining() < length){
			// 内容不足时重置并返回
			in.reset();
			return false;
		}
		
		IMinaMessage msg = messageFactory.createInstance(msgCode);
		int pos = in.position();
		msg.decode(in);
		int decLen = in.position() - pos;
		if (decLen < length){
			in.skip(length - decLen);
		}
		
		out.write(msg);
		return true;
	}

}
