/**
 * 
 */
package actorx.util;

import java.nio.ByteBuffer;
import java.util.List;

import actorx.ActorId;
import adata.Base;
import adata.Stream;

/**
 * @author Xiong
 * 包
 */
public abstract class Pack {
	protected ActorId sender;
	protected String type;
	// 当前未读过的序列化的字节位置
	protected int readPos;
	// 当前未读过的参数的索引
	protected int readIndex;
	protected List<Object> args;
	private static ThreadLocal<Stream> streamTLS = new ThreadLocal<Stream>();
	
	public ActorId getSender(){
		return sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void clear(){
		sender = null;
		type = null;
		readPos = 0;
		readIndex = 0;
		if (args != null){
			args.clear();
		}
	}
	
	public <T> T getRaw(){
		T t = getArg();
		if (t == null){
			++readIndex;
		}
		return t;
	}
	
	/**
	 * 获取指定类型的参数
	 * @return
	 * @throws Exception 
	 */
	protected <T extends Base> T get(ByteBuffer buffer, T t){
		T arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		((Base) t).read(stream);
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return t;
	}
		
	protected <T extends Base> T get(ByteBuffer buffer, Class<T> c){
		T arg = getArg();
		if (arg != null){
			return arg;
		}
		
		T t = null;
		try{
			t = c.newInstance();
		}catch (Exception e){
			throw new RuntimeException(e.getMessage());
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		t.read(stream);
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return t;
	}
	
	protected byte getByte(ByteBuffer buffer){
		Byte arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		byte o = stream.readInt8();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected boolean getBool(ByteBuffer buffer){
		Boolean arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		byte o = stream.readInt8();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o != 0;
	}
	
	protected short getShort(ByteBuffer buffer){
		Short arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		short o = stream.readInt16();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected int getInt(ByteBuffer buffer){
		Integer arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		int o = stream.readInt32();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected long getLong(ByteBuffer buffer){
		Long arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		long o = stream.readInt64();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected float getFloat(ByteBuffer buffer){
		Float arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		float o = stream.readFloat();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected double getDouble(ByteBuffer buffer){
		Double arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		double o = stream.readDouble();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	protected String getString(ByteBuffer buffer){
		String arg = getArg();
		if (arg != null){
			return arg;
		}
		
		Stream stream = getStream();
		int writePos = beginRead(stream, buffer);
		String o = stream.readString();
		readPos += stream.readLength();
		endRead(stream, buffer, writePos);
		return o;
	}
	
	/**
	 * 获取线程局部存储的adata Stream
	 * @return
	 */
	protected static Stream getStream(){
		Stream stream = streamTLS.get();
		if (stream == null){
			stream = new Stream();
			streamTLS.set(stream);
		}
		return stream;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getArg(){
		Object arg = args.get(readIndex);
		if (arg != null){
			++readIndex;
			T t = (T) arg;
			return t;
		}
		return null;
	}
	
	private int beginRead(Stream stream, ByteBuffer buffer){
		// 保存当前写入position
		int writePos = buffer.position();
		// 准备读
		buffer.flip();
		buffer.position(readPos);
		
		stream.setReadBuffer(buffer);
		return writePos;
	}
	
	private void endRead(Stream stream, ByteBuffer buffer, int writePos){
		stream.setReadBuffer(null);
		buffer.clear();
		buffer.position(writePos);
		++readIndex;
	}
}
