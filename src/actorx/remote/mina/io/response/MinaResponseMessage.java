/**
 * 
 */
package actorx.remote.mina.io.response;

import org.apache.mina.core.buffer.IoBuffer;

import actorx.remote.ErrorCode;
import actorx.remote.mina.io.AbstractMinaMessage;
import actorx.remote.mina.io.MinaMsgCode;

/**
 * @author Xiong
 *
 */
public class MinaResponseMessage extends AbstractMinaMessage {
	private ErrorCode errcode = ErrorCode.SUCCESS;

	public MinaResponseMessage(MinaMsgCode msgCode) {
		super(msgCode);
	}

	@Override
	protected final void decode0(IoBuffer ioBuffer) {
		errcode = ErrorCode.parse(ioBuffer.getShort());
		if (errcode == ErrorCode.SUCCESS){
			decodeBody(ioBuffer);
		}else{
			decodeError(ioBuffer);
		}
	}

	@Override
	protected final void encode0(IoBuffer ioBuffer) {
		// 写入状态码
		ioBuffer.putShort(errcode.getCode());
		// 写入具体消息数据
		if (errcode == ErrorCode.SUCCESS){
			encodeBody(ioBuffer);
		}else{
			encodeError(ioBuffer);
		}
	}
	
	protected void decodeError(IoBuffer ioBuffer){
	}
	
	protected void encodeError(IoBuffer ioBuffer){
	}

	public ErrorCode getErrcode() {
		return errcode;
	}

	public void setErrcode(ErrorCode errcode) {
		this.errcode = errcode;
	}
	
	// 以下实现PooledObject
	protected void initResponse(){
	}
	
	protected void resetResponse(){
	}
	
	protected final void initMessage(){
		errcode = ErrorCode.SUCCESS;
		initResponse();
	}
	
	protected final void resetMessage(){
		resetResponse();
	}
}
