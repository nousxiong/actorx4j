/**
 * 
 */
package actorx.remote.mina.io;

import java.nio.charset.CharacterCodingException;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;

import actorx.AbstractCustomMessage;
import actorx.ActorId;
import actorx.ICustomMessageFactory;
import actorx.Message;
import actorx.util.AdataUtils;

/**
 * @author Xiong
 *
 */
public abstract class AbstractMinaWrapMessage extends AbstractMinaMessage {
	private Map<String, ICustomMessageFactory> customMsgMap;
	private ActorId toAid;
	private Message wmsg;

	public AbstractMinaWrapMessage(MinaMsgCode msgCode) {
		super(msgCode);
	}

	@Override
	protected final void decode0(IoBuffer ioBuffer) {
		toAid = new ActorId();
		AdataUtils.read(toAid, ioBuffer);
		
		byte custom = ioBuffer.get();
		if (custom != 0){
			String ctype = "";
			try{
				ctype = ioBuffer.getPrefixedString(2, decoder);
			}catch (CharacterCodingException e){
				throw new RuntimeException(e);
			}
			
			ICustomMessageFactory factory = customMsgMap.get(ctype);
			wmsg = factory.createInstance();
		}else{
			wmsg = Message.make();
		}
		
		AdataUtils.read(wmsg, ioBuffer);
		decode1(ioBuffer);
	}

	@Override
	protected final void encode0(IoBuffer ioBuffer) {
		AdataUtils.write(toAid, ioBuffer);
		
		byte custom = 0;
		String ctype = "";
		if (wmsg instanceof AbstractCustomMessage){
			custom = 1;
			ctype = ((AbstractCustomMessage) wmsg).ctype();
		}
		ioBuffer.put(custom);
		if (custom != 0){
			try{
				ioBuffer.putPrefixedString(ctype, 2, encoder);
			}catch (CharacterCodingException e){
				throw new RuntimeException(e);
			}
		}
		
		AdataUtils.write(wmsg, ioBuffer);
		encode1(ioBuffer);
	}

	public ActorId getToAid() {
		return toAid;
	}

	public void setToAid(ActorId toAid) {
		this.toAid = toAid;
	}

	public Message getMessage() {
		return wmsg;
	}

	public void setMessage(Message smsg) {
		this.wmsg = smsg;
	}
	
	public void setCustomMessageMap(Map<String, ICustomMessageFactory> customMsgMap){
		this.customMsgMap = customMsgMap;
	}
	
	protected abstract void decode1(IoBuffer ioBuffer);
	protected abstract void encode1(IoBuffer ioBuffer);
}
