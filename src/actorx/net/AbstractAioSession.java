/**
 * 
 */
package actorx.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import actorx.ActorAddon;
import actorx.AtomCode;
import actorx.net.AioService.ReadHandler;
import actorx.net.AioService.WriteHandler;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:39:44
 *
 */
public abstract class AbstractAioSession {
	private AsynchronousSocketChannel skt;
	private static final int DEFAULT_READ_SIZE = 2048;
	private int readSize = DEFAULT_READ_SIZE;
	private ByteBuffer readBuffer = ByteBuffer.allocate(readSize * 2);
	private ByteBuffer writeBuffer;
	private int lastPosition = 0;
	private Queue<ByteBuffer> sendQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	private ActorAddon addon;
	private boolean msgReadEnd = false;
	private boolean writing = false;
	private boolean reading = false;
	private boolean closing = false;
	private boolean closed = false;
	private ReadHandler readHandler;
	private WriteHandler writeHandler;
	
	public AbstractAioSession(){
		this(DEFAULT_READ_SIZE);
	}
	
	public AbstractAioSession(int readSize){
		this.readSize = readSize;
	}
	
	public void setSocket(AsynchronousSocketChannel skt){
		this.skt = skt;
	}
	
	public AsynchronousSocketChannel getSocket(){
		return skt;
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	void onOpen(ActorAddon addon, ReadHandler readHandler, WriteHandler writeHandler){
		this.addon = addon;
		this.readHandler = readHandler;
		this.writeHandler = writeHandler;
		reading = true;
		asyncRead();
	}

	private void asyncRead(){
		skt.read(readBuffer, this, readHandler);
	}

	
	private boolean asyncWrite(){
		if (writeBuffer == null || !writeBuffer.hasRemaining()){
			do{
				writeBuffer = sendQueue.poll();
			}while (writeBuffer != null && !writeBuffer.hasRemaining());
		}
		
		if (writeBuffer != null){
			skt.write(writeBuffer, this, writeHandler);
			return true;
		}else{
			return false;
		}
	}
	
	void onRead(int result){
		if (result < 0){
			closesocket();
			addon.send(AioService.PEER_CLOSE, this);
			return;
		}
		
		try{
			if (result > 0){
				decode();
			}
			asyncRead();
		}catch (Throwable e){
			onReadFailed(e);
		}
	}
	
	void onReadFailed(Throwable exc){
		closesocket();
		addon.send(AioService.READ_ERR, this);
		addon.send(AtomCode.EXCEPT, exc, this);
	}
	
	void onWrite(int result){
		if (writeBuffer != null && !writeBuffer.hasRemaining()){
			writeBuffer = null;
		}
		
		if (!asyncWrite()){
			addon.send(AioService.WRITE_END, this);
		}
	}
	
	void onWriteFailed(Throwable exc){
		closesocket();
		addon.send(AioService.WRITE_ERR, this);
		addon.send(AtomCode.EXCEPT, exc, this);
	}
	
	void onFilterRecv(String type){
		switch (type){
			case AioService.PEER_CLOSE:
			case AioService.READ_ERR: {
				reading = false;
			}break;
			case AioService.WRITE_END: {
				if (closing){
					closesocket();
				}
			}
			case AioService.WRITE_ERR: {
				writing = false;
			}break;
		}
		tryClose();
	}
	
	private void tryClose(){
		if (!reading && !writing && !closed){
			closed = true;
			addon.send(AtomCode.CLOSE, this);
		}
	}
	
	void closesocket(){
		try {
			skt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void decode(){
		readBuffer.flip();
		readBuffer.position(lastPosition);
		while (true){
			int oldPos = readBuffer.position();
			doDecode(readBuffer);
			if (msgReadEnd){
				msgReadEnd = false;
				if (readBuffer.position() == oldPos) {
					throw new IllegalStateException("buffer is not consumed.");
				}
				
				if (!readBuffer.hasRemaining()){
					break;
				}
			}else{
				break;
			}
		}
		
		if (readBuffer.hasRemaining()){
			int emptySize = readBuffer.capacity() - readBuffer.limit();
			if (emptySize >= readSize){
				lastPosition = readBuffer.position();
				int limit = readBuffer.limit();
				readBuffer.clear();
				readBuffer.position(limit);
			}else{
				int remaining = readBuffer.remaining();
				if (readBuffer.capacity() - remaining >= readSize){
					readBuffer.compact();
				}else{
					ByteBuffer newBuffer = ByteBuffer.allocate(remaining + readSize);
					newBuffer.put(readBuffer.array(), readBuffer.position(), remaining);
					readBuffer = newBuffer;
				}
				lastPosition = 0;
			}
		}else{
			readBuffer.clear();
			lastPosition = 0;
		}
	}

	public boolean isClosing(){
		return closing;
	}
	
	public boolean isClosed(){
		return closed;
	}
	
	public void close(){
		closing = true;
		if (!writing){
			closesocket();
		}
		tryClose();
	}
	
	protected void write(ByteBuffer buffer){
		sendQueue.add(buffer);
		if (!writing){
			writing = true;
			asyncWrite();
		}
	}
	
	protected void sendReadResult(){
		addon.send(AtomCode.RECV, this);
		msgReadEnd = true;
	}
	
	protected void sendReadResult(Object arg){
		addon.send(AtomCode.RECV, this, arg);
		msgReadEnd = true;
	}
	
	protected void sendReadResult(Object arg1, Object arg2){
		addon.send(AtomCode.RECV, this, arg1, arg2);
		msgReadEnd = true;
	}
	
	protected void sendReadResult(Object arg1, Object arg2, Object arg3){
		addon.send(AtomCode.RECV, this, arg1, arg2, arg3);
		msgReadEnd = true;
	}
	
	protected abstract void doDecode(ByteBuffer buffer);
}
