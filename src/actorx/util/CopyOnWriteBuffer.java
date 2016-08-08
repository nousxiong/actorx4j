/**
 * 
 */
package actorx.util;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import actorx.CowBufferPool;
import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 * 写时拷贝buffer，写时使用引用计数来判断是否有共享需要拷贝
 */
public class CopyOnWriteBuffer implements INode {
	private ByteBuffer buffer;
	private AtomicInteger refCount = new AtomicInteger(0);
	
	public CopyOnWriteBuffer(int capacity){
		this.buffer = ByteBuffer.allocate(capacity);
	}
	
	/**
	 * 创建新buffer，数据从自身拷贝，decr自身引用计数
	 * @param src
	 * @return
	 */
	public CopyOnWriteBuffer copyOnWrite(){
		CopyOnWriteBuffer cowBuffer = CowBufferPool.get(buffer.capacity());
		cowBuffer.buffer.put(buffer.array(), 0, buffer.position());
		decrRef();
		return cowBuffer;
	}
	
	/**
	 * 复制一个ByteBuffer，共享内部byte[]
	 * @return
	 */
	public ByteBuffer duplicateBuffer(){
		return buffer.duplicate();
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
	public CopyOnWriteBuffer reserve(int length){
		if (length == 0){
			return this;
		}
		
		if (buffer.remaining() >= length){
			return this;
		}
		
		CopyOnWriteBuffer newBuffer = CowBufferPool.get(buffer.capacity() + length);
		newBuffer.getBuffer().put(buffer.array(), 0, buffer.position());
		return newBuffer;
	}
	
	/**
	 * 获取当前buffer
	 * @return
	 */
	public ByteBuffer getBuffer(){
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
		buffer.clear();
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
