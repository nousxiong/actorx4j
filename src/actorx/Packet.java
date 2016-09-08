/**
 * 
 */
package actorx;

import actorx.util.CopyOnWriteBuffer;
import actorx.util.Pack;
import adata.Base;

/**
 * @author Xiong
 * 简化的消息对象，临时和简单使用，只读用
 */
public class Packet extends Pack {
	private CopyOnWriteBuffer cowBuffer;
	
	public static final Packet NULL = null;

	@Override
	public void clear(){
		super.clear();
		if (cowBuffer != null){
			cowBuffer.decrRef();
			cowBuffer = null;
		}
	}
	
	/**
	 * 获取指定类型的参数
	 * @return
	 * @throws Exception 
	 */
	public <T extends Base> T get(T t){
		return super.get(cowBuffer, t);
	}
	
	public <T extends Base> T get(Class<T> c){
		return super.get(cowBuffer, c);
	}
	
	public byte getByte(){
		return super.getByte(cowBuffer);
	}
	
	public boolean getBool(){
		return super.getBool(cowBuffer);
	}
	
	public short getShort(){
		return super.getShort(cowBuffer);
	}
	
	public int getInt(){
		return super.getInt(cowBuffer);
	}
	
	public long getLong(){
		return super.getLong(cowBuffer);
	}
	
	public float getFloat(){
		return super.getFloat(cowBuffer);
	}
	
	public double getDouble(){
		return super.getDouble(cowBuffer);
	}
	
	public String getString(){
		return super.getString(cowBuffer);
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	public void set(Pack other, CopyOnWriteBuffer cowBuffer){
		this.sender = other.getSender();
		this.type = other.getType();
		if (this.cowBuffer != null){
			this.cowBuffer.decrRef();
		}
		this.cowBuffer = cowBuffer;
		this.argsCopyFrom(other);
		this.readIndex = 0;
		this.readPos = 0;
	}
	
}
