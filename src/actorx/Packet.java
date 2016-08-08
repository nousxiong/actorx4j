/**
 * 
 */
package actorx;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
		return super.get(getBuffer(), t);
	}
	
	public <T extends Base> T get(Class<T> c){
		return super.get(getBuffer(), c);
	}
	
	public byte getByte(){
		return super.getByte(getBuffer());
	}
	
	public boolean getBool(){
		return super.getBool(getBuffer());
	}
	
	public short getShort(){
		return super.getShort(getBuffer());
	}
	
	public int getInt(){
		return super.getInt(getBuffer());
	}
	
	public long getLong(){
		return super.getLong(getBuffer());
	}
	
	public float getFloat(){
		return super.getFloat(getBuffer());
	}
	
	public double getDouble(){
		return super.getDouble(getBuffer());
	}
	
	public String getString(){
		return super.getString(getBuffer());
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	public void set(ActorId sender, String type, CopyOnWriteBuffer cowBuffer, List<Object> args){
		this.sender = sender;
		this.type = type;
		if (this.cowBuffer != null){
			this.cowBuffer.decrRef();
		}
		this.cowBuffer = cowBuffer;
		if (args != null){
			if (this.args == null){
				this.args = new ArrayList<Object>(args.size());
			}
			this.args.clear();
			this.args.addAll(args);
		}else{
			if (this.args != null){
				this.args.clear();
			}
		}
		this.readIndex = 0;
		this.readPos = 0;
	}
	
	private ByteBuffer getBuffer(){
		return cowBuffer == null ? null : cowBuffer.getBuffer();
	}
	
}
