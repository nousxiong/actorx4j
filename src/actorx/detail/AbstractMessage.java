/**
 * 
 */
package actorx.detail;

import java.util.ArrayList;
import java.util.List;

import cque.AbstractNode;
import actorx.ActorId;
import actorx.AtomCode;
import actorx.adl.IAdlAdapter;
import actorx.util.AdataUtils;
import actorx.util.ContainerUtils;
import adata.Base;
import adata.Stream;

/**
 * @author Xiong
 * 抽象消息
 */
public abstract class AbstractMessage extends AbstractNode {
	protected ActorId sender = ActorId.NULLAID;
	protected String type = AtomCode.NULLTYPE;
	// 当前未读过的序列化的字节位置
	protected int readPos;
	// 当前未读过的参数的索引
	protected int readIndex;
	protected CowBuffer cowBuffer;
	
	public ActorId getSender(){
		return sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void clear(){
		sender = ActorId.NULLAID;
		type = AtomCode.NULLTYPE;
		readPos = 0;
		readIndex = 0;
		argsClear();
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
	protected <T extends Base> T getAdl(CowBuffer cowBuffer, T t){
		T arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		((Base) t).read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}

	protected <T extends Base> T getAdl(CowBuffer cowBuffer, Class<T> c){
		T arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		T t = null;
		try{
			t = c.newInstance();
		}catch (Throwable e){
			throw new RuntimeException(e.getMessage());
		}
		
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		t.read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}
	
	protected <T extends IAdlAdapter> T get(CowBuffer cowBuffer, T t){
		T arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		t.read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}
	
	protected <T extends IAdlAdapter> T get(CowBuffer cowBuffer, Class<T> c){
		T arg = getArg();
		if (arg != null){
			return arg;
		}
	
		byte[] buffer = cowBuffer.getBuffer();
		T t = null;
		try{
			t = c.newInstance();
		}catch (Throwable e){
			throw new RuntimeException(e.getMessage());
		}
		
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		t.read(stream);
		argsSet(readIndex, t);
		endRead(stream);
		return t;
	}
	
	protected byte getByte(CowBuffer cowBuffer){
		Byte arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		byte o = stream.readInt8();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected boolean getBool(CowBuffer cowBuffer){
		Boolean arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		byte o = stream.readInt8();
		argsSet(readIndex, o);
		endRead(stream);
		return o != 0;
	}
	
	protected short getShort(CowBuffer cowBuffer){
		Short arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		short o = stream.readInt16();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected int getInt(CowBuffer cowBuffer){
		Integer arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		int o = stream.readInt32();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected long getLong(CowBuffer cowBuffer){
		Long arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		long o = stream.readInt64();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected float getFloat(CowBuffer cowBuffer){
		Float arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		float o = stream.readFloat();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected double getDouble(CowBuffer cowBuffer){
		Double arg = getArg();
		if (arg != null){
			return arg;
		}

		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		double o = stream.readDouble();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
	}
	
	protected String getString(CowBuffer cowBuffer){
		String arg = getArg();
		if (arg != null){
			return arg;
		}
		
		byte[] buffer = cowBuffer.getBuffer();
		Stream stream = AdataUtils.getStream();
		beginRead(stream, buffer);
		String o = stream.readString();
		argsSet(readIndex, o);
		endRead(stream);
		return o;
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
		if (readPos == 0){
			readPos = Stream.fixSizeOfInt32();
		}
		stream.skipRead(readPos);
	}
	
	private void endRead(Stream stream){
		stream.setReadBuffer(null);
		readPos = stream.readLength();
		++readIndex;
	}
	
	/** 以下Args封装 */
	protected int argsSize = 0;
	private Object arg0;
	private Object arg1;
	private Object arg2;
	private Object arg3;
	private Object arg4;
	private Object arg5;
	private Object arg6;
	private Object arg7;
	private Object arg8;
	private Object arg9;
	private List<Object> args;
	private static final int MAX_ARG_CACHE_NUM = 10;
	
	protected void argsCopyFrom(AbstractMessage other){
		argsSize = other.argsSize;
		arg0 = other.arg0;
		arg1 = other.arg1;
		arg2 = other.arg2;
		arg3 = other.arg3;
		arg4 = other.arg4;
		arg5 = other.arg5;
		arg6 = other.arg6;
		arg7 = other.arg7;
		arg8 = other.arg8;
		arg9 = other.arg9;
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
	
	public int argsSize(){
		return argsSize;
	}
	
	protected void argsClear(){
		argsSize = 0;
		arg0 = arg1 = arg2 = arg3 = arg4 = null;
		arg5 = arg6 = arg7 = arg8 = arg9 = null;
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
		case 5: arg5 = o; break;
		case 6: arg6 = o; break;
		case 7: arg7 = o; break;
		case 8: arg8 = o; break;
		case 9: arg9 = o; break;
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
			throw new IndexOutOfBoundsException("index < 0 || index >= argsSize, index: "+index);
		}
		
		switch (index){
		case 0: return arg0;
		case 1: return arg1;
		case 2: return arg2;
		case 3: return arg3;
		case 4: return arg4;
		case 5: return arg5;
		case 6: return arg6;
		case 7: return arg7;
		case 8: return arg8;
		case 9: return arg9;
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
			throw new IndexOutOfBoundsException("index < 0 || index >= argsSize, index: "+index);
		}
		
		switch (index){
		case 0: arg0 = o; break;
		case 1: arg1 = o; break;
		case 2: arg2 = o; break;
		case 3: arg3 = o; break;
		case 4: arg4 = o; break;
		case 5: arg5 = o; break;
		case 6: arg6 = o; break;
		case 7: arg7 = o; break;
		case 8: arg8 = o; break;
		case 9: arg9 = o; break;
		default: 
			args.set(index - MAX_ARG_CACHE_NUM, o);
			break;
		}
	}
	
	protected boolean argsIsEmpty(){
		return argsSize == 0;
	}
}
