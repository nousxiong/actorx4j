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
import adata.Stream;
import cque.IFreer;
import cque.INode;
import cque.MpscNodePool;

/**
 * @author Xiong
 */
public class Message extends Pack implements INode {
	private CopyOnWriteBuffer cowBuffer;
	// 读取时使用的buffer，如果共享cowBuffer，则用ByteBuffer.duplicate来避免并发读带来的pos错误
	private ByteBuffer readBuffer;
	// 当前未序列化过的参数的索引
	private int writeIndex;
	
	/**
	 * 创建一个全新的消息对象，包括内部的写时拷贝Buffer
	 * @return
	 */
	public static Message make(){
		return make(MessagePool.getLocalPool(), CowBufferPool.getLocalPool());
	}
	
	/**
	 * 使用用户保存的本地池来创建消息
	 * @param msgPool
	 * @param cowBufferPool
	 * @return
	 */
	public static Message make(MpscNodePool<Message> msgPool, MpscNodePool<CopyOnWriteBuffer> cowBufferPool){
		Message msg = MessagePool.get(msgPool);
		if (msg.args == null){
			msg.args = new ArrayList<Object>();
		}
		return msg;
	}
	
	/**
	 * 根据旧的消息创建，共享旧消息的写时拷贝Buffer
	 * @param src
	 * @return
	 * @throws Exception 
	 */
	public static Message make(Message src){
		return make(src, MessagePool.getLocalPool());
	}

	/**
	 * 使用用户保存的本地池来创建消息
	 * @param src
	 * @param msgPool
	 * @return
	 */
	public static Message make(Message src, MpscNodePool<Message> msgPool){
		Message msg = makeEmpty(msgPool);
		msg.sender = src.sender;
		msg.type = src.type;
		
		if (src.args != null && !src.args.isEmpty() && src.cowBuffer == null){
			src.cowBuffer = CowBufferPool.get();
			src.readBuffer = src.cowBuffer.getBuffer();
		}
		
		if (src.cowBuffer != null){
			// 将src所有参数序列化后再做共享
			src.reserve();
			src.writeArgs();
			src.cowBuffer.incrRef();
			msg.cowBuffer = src.cowBuffer;
			if (src.readBuffer == src.cowBuffer.getBuffer()){
				src.readBuffer = src.cowBuffer.duplicateBuffer();
			}
			msg.readBuffer = msg.cowBuffer.duplicateBuffer();
		}
		
		msg.writeIndex = src.writeIndex;
		if (src.args != null && !src.args.isEmpty()){
			msg.args = src.makeCopyArgs();
		}
		return msg;
	}
	
	/**
	 * 创建一个空消息（无写时拷贝Buffer）
	 * @return
	 */
	public static Message makeEmpty(){
		return makeEmpty(MessagePool.getLocalPool());
	}
	
	/**
	 * 使用用户保存的本地池来创建消息
	 * @param msgPool
	 * @return
	 */
	public static Message makeEmpty(MpscNodePool<Message> msgPool){
		return MessagePool.get(msgPool);
	}
	
	public void setSender(ActorId sender){
		this.sender = sender;
	}
	
	public void setType(String type){
		this.type = type;
	}

	///------------------------------------------------------------------------
	/// 以下是put api
	///------------------------------------------------------------------------
	/**
	 * 写入指定类型的参数
	 * @param t
	 */
	public <T> Message put(T t){
		copyOnWrite();
		args.add(t);
		return this;
	}
	
	///------------------------------------------------------------------------
	/// 以下是get api
	///------------------------------------------------------------------------
	/**
	 * 获取指定类型的参数
	 * @return
	 */
	public <T extends Base> T get(T t){
		return super.get(readBuffer, t);
	}
	
	/**
	 * 获取指定类型并可能使用Class来创建新实例的参数
	 * @param c
	 * @return
	 */
	public <T extends Base> T get(Class<T> c){
		return super.get(readBuffer, c);
	}
	
	public byte getByte(){
		return super.getByte(readBuffer);
	}
	
	public boolean getBool(){
		return super.getBool(readBuffer);
	}
	
	public short getShort(){
		return super.getShort(readBuffer);
	}
	
	public int getInt(){
		return super.getInt(readBuffer);
	}
	
	public long getLong(){
		return super.getLong(readBuffer);
	}
	
	public float getFloat(){
		return super.getFloat(readBuffer);
	}
	
	public double getDouble(){
		return super.getDouble(readBuffer);
	}
	
	public String getString(){
		return super.getString(readBuffer);
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	/**
	 * 转移相关数据到新Packet中
	 * @note 调用此方法后，消息本身需要释放，已经不合法
	 * @return
	 */
	public Packet move(Packet pkt){
		CopyOnWriteBuffer buffer = null;
		if (cowBuffer != null){
			buffer = cowBuffer;
			cowBuffer = null;
		}
		readBuffer = null;
		
		if (pkt == null){
			pkt = new Packet();
		}
		pkt.set(sender, type, buffer, args);
		return pkt;
	}
	
	/**
	 * 创建args的副本
	 * @return
	 */
	private List<Object> makeCopyArgs(){
		if (args == null){
			return null;
		}
		
		return new ArrayList<Object>(args);
	}
	
	private void copyOnWrite(){
		if (cowBuffer != null && cowBuffer.getRefCount() > 1){
			if (!reserve()){
				// 写时拷贝，重新创建一个新buffer，拷贝旧数据，decr旧buffer引用计数
				cowBuffer = cowBuffer.copyOnWrite();
			}
			// 包括args的拷贝
			args = makeCopyArgs();
			// 将能序列化的对象序列化
			writeArgs();
			// 设置读取buffer
			readBuffer = cowBuffer.getBuffer();
		}
		
		if (args == null){
			args = new ArrayList<Object>();
		}
	}
	
	private boolean reserve(){
		int length = argsLength();
		CopyOnWriteBuffer newBuffer = cowBuffer.reserve(length);
		if (newBuffer != cowBuffer){
			readBuffer = newBuffer.getBuffer();
			cowBuffer.decrRef();
			cowBuffer = newBuffer;
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 需要序列化的参数的长度
	 * @return
	 */
	private int argsLength(){
		if (args == null){
			return 0;
		}
		
		int length = 0;
		for (int i=writeIndex; i<args.size(); ++i){
			Object arg = args.get(i);
			if (arg != null){
				if (arg instanceof Base){
					length += ((Base) arg).sizeOf();
				}else if (arg instanceof Byte){
					length += Stream.sizeOfInt8((byte) arg);
				}else if (arg instanceof Boolean){
					byte o = (byte) ((boolean) arg ? 1 : 0);
					length += Stream.sizeOfInt8(o);
				}else if (arg instanceof Short){
					length += Stream.sizeOfInt16((short) arg);
				}else if (arg instanceof Integer){
					length += Stream.sizeOfInt32((int) arg);
				}else if (arg instanceof Long){
					length += Stream.sizeOfInt64((long) arg);
				}else if (arg instanceof Float){
					length += Stream.sizeOfFloat((float) arg);
				}else if (arg instanceof Double){
					length += Stream.sizeOfDouble((double) arg);
				}else if (arg instanceof String){
					length += Stream.sizeOfString((String) arg);
				}
			}
		}
		return length;
	}
	
	private void writeArgs(){
		if (args == null){
			return;
		}
		
		for (int i=writeIndex; i<args.size(); ++i, ++writeIndex){
			Object arg = args.get(i);
			if (writeArg(arg)){
				args.set(i, null);
			}
		}
	}
	
	private boolean writeArg(Object arg){
		if (arg == null){
			return true;
		}
		
		Stream stream = getStream();
		boolean isAdata = true;
		try{
			ByteBuffer buffer = cowBuffer.getBuffer();
			if (arg instanceof Base){
				Base o = (Base) arg;
				stream.setWriteBuffer(buffer);
				o.write(stream);
			}else if (arg instanceof Byte){
				Byte o = (Byte) arg;
				stream.setWriteBuffer(buffer);
				stream.writeInt8(o);
			}else if (arg instanceof Boolean){
				Byte o = (byte) ((boolean) arg ? 1 : 0);
				stream.setWriteBuffer(buffer);
				stream.writeInt8(o);
			}else if (arg instanceof Short){
				Short o = (Short) arg;
				stream.setWriteBuffer(buffer);
				stream.writeInt16(o);
			}else if (arg instanceof Integer){
				Integer o = (Integer) arg;
				stream.setWriteBuffer(buffer);
				stream.writeInt32(o);
			}else if (arg instanceof Long){
				Long o = (Long) arg;
				stream.setWriteBuffer(buffer);
				stream.writeInt64(o);
			}else if (arg instanceof Float){
				Float o = (Float) arg;
				stream.setWriteBuffer(buffer);
				stream.writeFloat(o);
			}else if (arg instanceof Double){
				Double o = (Double) arg;
				stream.setWriteBuffer(buffer);
				stream.writeDouble(o);
			}else if (arg instanceof String){
				String o = (String) arg;
				stream.setWriteBuffer(buffer);
				stream.writeString(o);
			}else{
				isAdata = false;
			}
		}finally{
			stream.setWriteBuffer(null);
		}
		return isAdata;
	}
	
	
	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;

	@Override
	public void release(){
		if (freer != null){
			if (cowBuffer != null){
				cowBuffer.decrRef();
				cowBuffer = null;
			}
			freer.free(this);
			freer = null;
		}
	}

	@Override
	public INode getNext(){
		return next;
	}

	@Override
	public INode fetchNext(){
		INode nx = next;
		next = null;
		return nx;
	}

	@Override
	public void onFree(){
		super.clear();
		readBuffer = null;
		writeIndex = 0;
		next = null;
		freer = null;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.next = null;
		this.type = MsgType.NULLTYPE;
	}

	@Override
	public void setNext(INode next){
		this.next = next;
	}
}
