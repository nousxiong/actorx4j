/**
 * 
 */
package actorx.util;

import java.util.concurrent.atomic.AtomicInteger;

import actorx.CowBufferPool;
import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 * 写时拷贝buffer，写时使用引用计数来判断是否有共享需要拷贝
 */
public class CopyOnWriteBuffer implements INode {
	private static final int DEFAULT_BUFFER_SIZE = 64;
	private byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
	private int size = 0;
	private AtomicInteger refCount = new AtomicInteger(0);
	
	/**
	 * 创建新buffer，数据从自身拷贝，decr自身引用计数
	 * @param src
	 * @return
	 */
	public CopyOnWriteBuffer copyOnWrite(){
		CopyOnWriteBuffer cowBuffer = CowBufferPool.get();
		cowBuffer.write(buffer, 0, size);
		decrRef();
		return cowBuffer;
	}
	
	/**
	 * 将自己的buffer转移出去，如果有共享，写时拷贝buffer
	 * @return
	 */
	public byte[] moveOrCopy(){
		byte[] buffer = null;
		if (getRefCount() > 1){
			buffer = new byte[size];
			System.arraycopy(this.buffer, 0, buffer, 0, size);
		}else{
			buffer = this.buffer;
			this.buffer = new byte[DEFAULT_BUFFER_SIZE];
			size = 0;
		}
		return buffer;
	}

	/**
	 * 当前size
	 * @return
	 */
	public int size(){
		return size;
	}
	
	/**
	 * 获取当前buffer的容量
	 * @return
	 */
	public int getCapacity(){
		return buffer.length;
	}
	
	/**
	 * 返回当前的引用计数
	 * @return
	 */
	public int getRefCount(){
		return refCount.get();
	}
	
	/**
	 * 向buffer中写入数据
	 * @param bytes
	 * @return 实际写入的长度
	 */
	public int write(byte[] bytes){
		return write(bytes, 0, bytes.length);
	}
	
	/**
	 * 向buffer中写入数据
	 * @param src
	 * @param srcPos
	 * @param length
	 * @return 实际写入的长度
	 */
	public int write(byte[] src, int srcPos, int length){
		if (srcPos >= src.length || length == 0){
			return 0;
		}
		
		int remainLength = src.length - srcPos;
		int writeLength = remainLength > length ? length : remainLength;
		if (buffer.length - size < writeLength){
			byte[] newBuffer = new byte[buffer.length + writeLength];
			System.arraycopy(buffer, 0, newBuffer, 0, size);
			buffer = newBuffer;
		}
		
		System.arraycopy(src, srcPos, buffer, size, writeLength);
		size += writeLength;
		return writeLength;
	}
	
	/**
	 * 略过写
	 * @param length
	 */
	public int skipWrite(int length){
		if (length == 0){
			return 0;
		}
		
		if (buffer.length - size < length){
			byte[] newBuffer = new byte[buffer.length + length];
			System.arraycopy(buffer, 0, newBuffer, 0, size);
			buffer = newBuffer;
		}
		size += length;
		return length;
	}
	
	/**
	 * 读取指定偏移量和长度的数据到指定的byte数组中
	 * @param dest
	 * @param offset
	 * @return 实际读取的长度
	 */
	public int read(byte[] dest, int offset){
		return read(dest, 0, offset, dest.length);
	}
	
	/**
	 * 读取指定偏移量和长度的数据到指定的byte数组中
	 * @param dest
	 * @param destPos
	 * @param offset
	 * @param length
	 * @return 实际读取的长度
	 */
	public int read(byte[] dest, int destPos, int offset, int length){
		if (destPos >= dest.length || length == 0){
			return 0;
		}
		
		if (size - offset < length){
			length = size - offset;
		}
		
		System.arraycopy(buffer, offset, dest, destPos, length);
		return length;
	}
	
	/**
	 * 略过读
	 * @param offset
	 * @param length
	 * @return 实际读取的长度
	 */
	public int skipRead(int offset, int length){
		if (size - offset < length){
			length = size - offset;
		}
		return length;
	}
	
	/**
	 * 获取当前buffer
	 * @return
	 */
	public byte[] getBuffer(){
		return buffer;
	}
	
	/**
	 * 增加引用
	 * @return 返回增加后的引用计数
	 */
	public int incrRef(){
		return refCount.incrementAndGet();
	}
	
	/**
	 * 通过减少引用计数来释放自身
	 * @return 减少后的引用计数
	 * @note 如果返回0，说明对象已经被释放，不得再访问其任何方法
	 */
	public int decrRef(){
		int ref = refCount.decrementAndGet();
		if (ref == 0){
			// 释放自己
			release();
		}
		return ref;
	}
	
	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;

	@Override
	public void release(){
		if (freer != null){
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
		next = null;
		freer = null;
		size = 0;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.next = null;
		this.incrRef();
	}

	@Override
	public void setNext(INode next){
		this.next = next;
	}
}
