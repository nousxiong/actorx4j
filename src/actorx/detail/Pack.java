/**
 * 
 */
package actorx.detail;

import java.util.ArrayList;
import java.util.List;

import actorx.ActorId;
import actorx.MsgType;
import actorx.util.ContainerUtils;
import adata.Base;
import adata.Stream;

/**
 * @author Xiong
 * 包
 */
public abstract class Pack {
	protected ActorId sender = ActorId.NULLAID;
	protected String type = MsgType.NULLTYPE;
	// 当前未读过的序列化的字节位置
	protected int readPos;
	// 当前未读过的参数的索引
	protected int readIndex;
	private static ThreadLocal<Stream> streamTLS = new ThreadLocal<Stream>();
	
	public ActorId getSender(){
		return sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void clear(){
		sender = ActorId.NULLAID;
		type = MsgType.NULLTYPE;
		readPos = 0;
		readIndex = 0;
		argsClear();
	}
	
	public void resetRead(){
		readIndex = 0;
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
	protected <T extends Base> T get(CopyOnWriteBuffer cowBuffer, T t){
		T arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		((Base) t).read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}
		
	protected <T extends Base> T get(CopyOnWriteBuffer cowBuffer, Class<T> c){
		T arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		T t = null;
		try{
			t = c.newInstance();
		}catch (Exception e){
			throw new RuntimeException(e.getMessage());
		}
		
		Stream stream = getStream();
		beginRead(stream, buffer);
		t.read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}
	
	protected byte getByte(CopyOnWriteBuffer cowBuffer){
		Byte arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		byte o = stream.readInt8();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected boolean getBool(CopyOnWriteBuffer cowBuffer){
		Boolean arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		byte o = stream.readInt8();
		argsSet(readIndex, o);
		endRead(stream);
		return o != 0;
	}
	
	protected short getShort(CopyOnWriteBuffer cowBuffer){
		Short arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		short o = stream.readInt16();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected int getInt(CopyOnWriteBuffer cowBuffer){
		Integer arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		int o = stream.readInt32();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected long getLong(CopyOnWriteBuffer cowBuffer){
		Long arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		long o = stream.readInt64();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected float getFloat(CopyOnWriteBuffer cowBuffer){
		Float arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		float o = stream.readFloat();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected double getDouble(CopyOnWriteBuffer cowBuffer){
		Double arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		double o = stream.readDouble();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected String getString(CopyOnWriteBuffer cowBuffer){
		String arg = getArg();
		if (arg != null){
			return arg;
		}
		
		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = getStream();
		beginRead(stream, buffer);
		String o = stream.readString();
		argsSet(readIndex, o);
		endRead(stream);
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
		Object arg = argsGet(readIndex);
		if (arg != null){
			++readIndex;
			T t = (T) arg;
			return t;
		}
		return null;
	}
	
	private void beginRead(Stream stream, byte[] buffer){
		stream.clearRead();
		stream.setReadBuffer(buffer);
		stream.skipRead(readPos);
	}
	
	private void endRead(Stream stream){
		stream.setReadBuffer(null);
		readPos = stream.readLength();
		++readIndex;
	}
	
	/** 以下Args封装 */
	private int argsSize = 0;
	private Object arg0;
	private Object arg1;
	private Object arg2;
	private Object arg3;
	private Object arg4;
	private List<Object> args;
	private static final int MAX_ARG_CACHE_NUM = 5;
	
	protected void argsCopyFrom(Pack other){
		argsSize = other.argsSize;
		arg0 = other.arg0;
		arg1 = other.arg1;
		arg2 = other.arg2;
		arg3 = other.arg3;
		arg4 = other.arg4;
		if (!ContainerUtils.isEmpty(other.args)){
			if (args == null){
				args = new ArrayList<Object>(other.args.size());
			}else{
				args.clear();
			}
			args.addAll(other.args);
		}else{
			if (args != null){
				args.clear();
			}
		}
	}
	
	protected void argsClear(){
		argsSize = 0;
		arg0 = arg1 = arg2 = arg3 = arg4 = null;
		if (!ContainerUtils.isEmpty(args)){
			args.clear();
		}
	}
	
	protected void argsAdd(Object o){
		switch (argsSize){
		case 0: arg0 = o; break;
		case 1: arg1 = o; break;
		case 2: arg2 = o; break;
		case 3: arg3 = o; break;
		case 4: arg4 = o; break;
		default:
			if (args == null){
				args = new ArrayList<Object>(1);
			}
			args.add(o);
			break;
		}
		++argsSize;
	}
	
	protected Object argsGet(int index){
		if (index < 0 || index >= argsSize){
			throw new IndexOutOfBoundsException("index < 0 || index >= argsSize");
		}
		
		switch (index){
		case 0: return arg0;
		case 1: return arg1;
		case 2: return arg2;
		case 3: return arg3;
		case 4: return arg4;
		default: 
			if (!ContainerUtils.isEmpty(args)){
				return args.get(index - MAX_ARG_CACHE_NUM);
			}
			break;
		}
		return null;
	}
	
	protected void argsSet(int index, Object o){
		if (index < 0 || index >= argsSize){
			throw new IndexOutOfBoundsException("index < 0 || index >= argsSize");
		}
		
		switch (index){
		case 0: arg0 = o; break;
		case 1: arg1 = o; break;
		case 2: arg2 = o; break;
		case 3: arg3 = o; break;
		case 4: arg4 = o; break;
		default: 
			args.set(index - MAX_ARG_CACHE_NUM, o);
			break;
		}
	}
	
	protected boolean argsIsEmpty(){
		return argsSize == 0;
	}
	
	protected int argsSize(){
		return argsSize;
	}
}
