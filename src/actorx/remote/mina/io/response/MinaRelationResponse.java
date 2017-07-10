/**
 * 
 */
package actorx.remote.mina.io.response;

import org.apache.mina.core.buffer.IoBuffer;

import actorx.ActorExit;
import actorx.remote.mina.io.MinaMsgCode;
import actorx.util.AdataUtils;

/**
 * @author Xiong
 *
 */
public class MinaRelationResponse extends MinaResponseMessage {
	private ActorExit axExit;

	public MinaRelationResponse(MinaMsgCode msgCode) {
		super(msgCode);
	}
	
	@Override
	protected void decodeBody(IoBuffer ioBuffer) {
	}
	
	@Override
	protected void encodeBody(IoBuffer ioBuffer) {
	}
	
	@Override
	protected void decodeError(IoBuffer ioBuffer){
		if (axExit == null){
			axExit = new ActorExit();
		}
		AdataUtils.read(axExit, ioBuffer);
	}
	
	@Override
	protected void encodeError(IoBuffer ioBuffer){
		AdataUtils.write(axExit, ioBuffer);
	}

	public ActorExit getActorExit() {
		return axExit;
	}

	public void setActorExit(ActorExit axExit) {
		this.axExit = axExit;
	}

}
