/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;

import actorx.util.CopyOnWriteBuffer;
import cque.IFreer;
import cque.INode;
import cque.MpscNodePool;

/**
 * @author Xiong
 */
public class Message implements INode {
	private ActorId sender;
	private String type;
	private CopyOnWriteBuffer cowBuffer;
	private int cowReadSize;
	private List<Object> args;
	
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
		msg.cowBuffer = CowBufferPool.get(cowBufferPool);
		if (msg.args == null){
			msg.args = new ArrayList<Object>();
		}
		return msg;
	}
	
	/**
	 * 根据旧的消息创建，共享旧消息的写时拷贝Buffer
	 * @param src
	 * @return
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
		Message msg = MessagePool.get(msgPool);
		msg.sender = src.sender;
		msg.type = src.type;
		
		src.cowBuffer.incrRef();
		msg.cowBuffer = src.cowBuffer;
		
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
	
	/**
	 * 转移相关数据到新Packet中
	 * @return
	 */
	public Packet move(Packet pkt){
		int size = 0;
		byte[] buffer = null;
		if (cowBuffer != null){
			size = cowBuffer.size();
			buffer = cowBuffer.moveOrCopy();
		}
		List<Object> args = this.args;
		this.args = null;
		
		if (pkt == null){
			pkt = new Packet();
		}
		pkt.set(sender, type, buffer, size, args);
		return pkt;
	}
	
	public ActorId getSender(){
		return sender;
	}
	
	public void setSender(ActorId sender){
		this.sender = sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	/**
	 * 写入指定类型的参数
	 * @param t
	 */
	public <T> void write(T t){
		if (cowBuffer == null){
			cowBuffer = CowBufferPool.get();
		}else if (cowBuffer.getRefCount() > 1){
			// 写时拷贝，重新创建一个新buffer，拷贝旧数据，decr旧buffer引用计数
			cowBuffer = cowBuffer.copyOnWrite();
			// 包括args的拷贝
			args = makeCopyArgs();
		}
		
		int writeSize = cowBuffer.size();
		if (cowBuffer.skipWrite(1) != 1){
			return;
		}
		
		if (args == null){
			args = new ArrayList<Object>();
		}
		
		byte[] buffer = cowBuffer.getBuffer();
		buffer[writeSize] = (byte) args.size();
		args.add(t);
	}
	
	/**
	 * 获取指定类型的参数
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T read(){
		if (cowBuffer == null){
			return null;
		}
		
		if (cowBuffer.skipRead(cowReadSize, 1) == 0){
			return null;
		}
		
		byte[] buffer = cowBuffer.getBuffer();
		byte i = buffer[cowReadSize];
		cowReadSize += 1;
		return (T) args.get(i);
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
		sender = null;
		type = null;
		cowReadSize = 0;
		if (args != null){
			args.clear();
		}
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
