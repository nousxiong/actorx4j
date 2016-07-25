/**
 * 
 */
package actorx;

import java.util.List;

/**
 * @author Xiong
 * 简化的消息对象，临时和简单使用，只读用
 */
public class Packet {
	private ActorId sender;
	private String type;
	private byte[] buffer;
	private int size;
	private int readSize = 0;
	private List<Object> args;
	
	public static final Packet NULL = null;

	public ActorId getSender() {
		return sender;
	}

	public String getType() {
		return type;
	}

	/**
	 * 读取指定类型的参数
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T read(){
		if (readSize == size || buffer == null){
			return null;
		}
		
		byte i = buffer[readSize];
		readSize += 1;
		return (T) args.get(i);
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	public void set(ActorId sender, String type, byte[] buffer, int size, List<Object> args){
		this.sender = sender;
		this.type = type;
		this.buffer = buffer;
		this.size = size;
		this.args = args;
		this.readSize = 0;
	}
}
