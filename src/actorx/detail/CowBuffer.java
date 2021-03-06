/**
 * 
 */
package actorx.detail;

import java.util.concurrent.atomic.AtomicInteger;

import cque.AbstractNode;
import actorx.util.CowBufferPool;

/**
 * @author Xiong
 * 写时拷贝buffer，写时使用引用计数来判断是否有共享需要拷贝
 */
public class CowBuffer extends AbstractNode {
	private byte[] buffer;
	private int size;
	private AtomicInteger refCount = new AtomicInteger(0);
	
	public CowBuffer(int capacity){
		this.buffer = new byte[capacity];
		this.size = 0;
	}
	
	/**
	 * 创建新buffer，数据从自身拷贝，decr自身引用计数
	 * @param src
	 * @return
	 */
	public CowBuffer copyOnWrite(){
		CowBuffer cowBuffer = CowBufferPool.borrowObject(buffer.length);
		copy(buffer, cowBuffer, size);
		decrRef();
		return cowBuffer;
	}
	
	/**
	 * 返回当前的引用计数
	 * @return
	 */
	public int getRefCount(){
		return refCount.get();
	}
	
	/**
	 * 扩展Length长度的数据，如果剩余长度不足，会尝试扩展buffer
	 * @param length
	 * @return 如果未扩展返回自身，如果扩展了，返回新写时拷贝buffer
	 */
	public CowBuffer reserve(int length){
		if (length == 0){
			return this;
		}
		
		if (buffer.length - size >= length){
			return this;
		}
		
		CowBuffer newBuffer = CowBufferPool.borrowObject(size + length);
		copy(buffer, newBuffer, size);
		return newBuffer;
	}
	
	/**
	 * 获取当前buffer
	 * @return
	 */
	public byte[] getBuffer(){
		return buffer;
	}
	
	/**
	 * 获取当前buffer的大小
	 * @return
	 */
	public int size(){
		return size;
	}
	
	public void write(int len){
		if (size + len > buffer.length){
			size = buffer.length;
		}else{
			size += len;
		}
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
	
	/**
	 * 拷贝数据到cowBuffer上
	 * @param src
	 * @param dest
	 * @param srcOff
	 * @param len
	 */
	public static void copy(byte[] src, CowBuffer dest, int srcOff, int len){
		if (len > 0){
			System.arraycopy(src, srcOff, dest.buffer, 0, len);
		}
		dest.size = len;
	}
	
	private static void copy(byte[] src, CowBuffer dest, int len){
		copy(src, dest, 0, len);
	}

	@Override
	protected void initNode(){
		this.incrRef();
	}

	@Override
	protected void resetNode(){
		size = 0;
	}
}
