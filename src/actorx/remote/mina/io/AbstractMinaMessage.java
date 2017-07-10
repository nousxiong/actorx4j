/**
 * 
 */
package actorx.remote.mina.io;

import actorx.util.CodecUtils;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import cque.IPooledObject;
import cque.IRecycler;

/**
 * @author Xiong
 * @creation 2017年2月10日下午7:00:34
 *
 */
public abstract class AbstractMinaMessage implements IMinaMessage, IPooledObject {
	// msg code
	private MinaMsgCode msgCode;
	private int lengthHint = 256;
	protected CharsetEncoder encoder;
	protected CharsetDecoder decoder;
	
	public AbstractMinaMessage(MinaMsgCode msgCode){
		this.msgCode = msgCode;
	}

	public void setLengthHint(int lengthHint) {
		this.lengthHint = lengthHint;
	}

	@Override
	public MinaMsgCode getMsgCode() {
		return msgCode;
	}

	@Override
	public String toLogString() {
		return "msgCode="+msgCode;
	}

	@Override
	public final void decode(IoBuffer ioBuffer) {
		decoder = CodecUtils.getDecoder();
		decode0(ioBuffer);
	}

	@Override
	public final IoBuffer encode() {
		encoder = CodecUtils.getEncoder();
		IoBuffer out = IoBuffer.allocate(lengthHint);
		out.setAutoExpand(true);
		
		// 写入数据长度（消息头）
		out.putUnsignedShort(0);
		// 写入消息码（消息头）
		out.putUnsignedShort(msgCode.getCode());
		// 写入子类内容
		encode0(out);
		// 重新修改数据长度
		int length = out.position() - HEADER_LENGTH;
		out.putUnsignedShort(0, length);
		
		out.flip();
		return out;
	}

	protected void decodeBody(IoBuffer ioBuffer) {
	}
	
	protected void encodeBody(IoBuffer ioBuffer) {
	}
	
	protected abstract void decode0(IoBuffer ioBuffer);
	protected abstract void encode0(IoBuffer ioBuffer);
	

	// 以下实现IPooledObject
	private IRecycler recycler;
	
	protected void initMessage(){
	}
	
	protected void resetMessage(){
	}
	
	@Override
	public final void onBorrowed(IRecycler recycler) {
		this.recycler = recycler;
		lengthHint = 256;
		initMessage();
	}

	@Override
	public final void onReturn() {
		resetMessage();
		encoder = null;
		decoder = null;
	}

	@Override
	public final void release() {
		if (recycler != null){
			recycler.returnObject(this);
		}
	}
}
