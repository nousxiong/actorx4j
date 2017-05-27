/**
 * 
 */
package actorx;

import actorx.adl.IAdlAdapter;
import actorx.detail.CowBuffer;
import actorx.detail.IMail;
import actorx.detail.AbstractMessage;
import actorx.util.AdataUtils;
import actorx.util.Atom;
import actorx.util.CowBufferPool;
import actorx.util.MessagePool;
import adata.Base;
import adata.Stream;

/**
 * @author Xiong
 */
public class Message extends AbstractMessage implements /*INode, */IMail, IAdlAdapter {
	// null消息
	public static final Message NULL = null;
	
	// 空消息
	public static final Message NULLMSG = new Message();

	// 当前未序列化过的参数的索引
	private int writeIndex;
	
	/**
	 * 创建一个全新的消息对象
	 * @return
	 */
	public static Message make(){
		return MessagePool.borrowObject();
	}

	/**
	 * 根据旧的消息创建，共享旧消息的写时拷贝Buffer
	 * @param src
	 * @param msgPool
	 * @return
	 */
	public static Message make(Message src){
		Message msg = make();
		msg.sender = src.sender;
		msg.type = src.type;
		
//		if (!src.argsIsEmpty() && src.cowBuffer == null){
//			src.cowBuffer = CowBufferPool.borrowObject();
//		}
		
		if (src.cowBuffer != null || !src.argsIsEmpty()){
			// 将src所有参数序列化后再做共享
//			int argsLen = src.argsLength();
//			int length = src.isBufferEmpty() ? Stream.fixSizeOfInt32() : 0;
//			src.reserve(argsLen + length);
			src.writeArgs(AdataUtils.getStream());
			
//			int length = src.argsLength();
//			int metaLen = src.metaLength(length);
//			src.reserve(length + metaLen);
//			src.writeArgs(length);
			src.cowBuffer.incrRef();
			msg.cowBuffer = src.cowBuffer;
		}
		
		msg.writeIndex = src.writeIndex;
		if (!src.argsIsEmpty()){
			msg.argsCopyFrom(src);
		}
		return msg;
	}
	
//	public static Message make(byte[] bytes){
//		return make(bytes, 0, bytes.length, MessagePool.getLocalPool());
//	}
//	
//	public static Message make(byte[] bytes, int offset, int length){
//		return make(bytes, offset, length, MessagePool.getLocalPool());
//	}
//	
//	public static Message make(byte[] bytes, MpscNodePool<Message> msgPool){
//		return make(bytes, 0, bytes.length, msgPool);
//	}
//	
//	public static Message make(byte[] bytes, int offset, int length, MpscNodePool<Message> msgPool){
//		Message msg = make(msgPool);
//		if (bytes == null){
//			throw new NullPointerException();
//		}
//		
//		if (length <= 0 || offset >= bytes.length){
//			throw new ArrayIndexOutOfBoundsException();
//		}
//		
//		msg.cowBuffer = CowBufferPool.get(length);
//		byte[] cowBytes = msg.cowBuffer.getBuffer();
//		System.arraycopy(bytes, offset, cowBytes, 0, length);
//		msg.cowBuffer.write(length);
//		msg.writeIndex = Integer.MAX_VALUE;
//		return msg;
//	}
	
	public void setSender(ActorId sender){
		if (sender == null){
			sender = ActorId.NULLAID;
		}
		this.sender = sender;
	}
	
	public void setType(String type){
		if (type == null){
			type = AtomCode.NULLTYPE;
		}
		this.type = type;
	}
	
	public void resetRead(){
		readIndex = 0;
		if (writeIndex > 0){
			// 说明曾经序列化过，可能某些arg已经null，所以需要将readPos重置到0，重新走序列化
			readPos = 0;
		}
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
		argsAdd(t);
		return this;
	}
	
	///------------------------------------------------------------------------
	/// 以下是get api
	///------------------------------------------------------------------------
	/**
	 * 获取指定类型的、直接从adl生成的参数
	 * @return
	 */
	public <T extends Base> T getAdl(T t){
		return super.getAdl(cowBuffer, t);
	}
	
	/**
	 * 获取指定类型并可能使用Class来创建新实例的、直接从adl生成的参数
	 * @param c
	 * @return
	 */
	public <T extends Base> T getAdl(Class<T> c){
		return super.getAdl(cowBuffer, c);
	}
	
	/**
	 * 获取指定类型的、从{@link IAdlAdapter}继承的参数
	 * @return
	 */
	public <T extends IAdlAdapter> T get(T t){
		return super.get(cowBuffer, t);
	}
	
	/**
	 * 获取指定类型并可能使用Class来创建新实例的、从{@link IAdlAdapter}继承的参数
	 * @param c
	 * @return
	 */
	public <T extends IAdlAdapter> T get(Class<T> c){
		return super.get(cowBuffer, c);
	}
	
	public byte getByte(){
		return super.getByte(cowBuffer);
	}
	
	public boolean getBool(){
		return super.getBool(cowBuffer);
	}
	
	public short getShort(){
		return super.getShort(cowBuffer);
	}
	
	public int getInt(){
		return super.getInt(cowBuffer);
	}
	
	public long getLong(){
		return super.getLong(cowBuffer);
	}
	
	public float getFloat(){
		return super.getFloat(cowBuffer);
	}
	
	public double getDouble(){
		return super.getDouble(cowBuffer);
	}
	
	public String getString(){
		return super.getString(cowBuffer);
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	/**
	 * 转移相关数据到新Message中
	 * @note 调用此方法后，消息src被释放，已经不合法
	 * @return dest
	 */
	static Message move(Message src, Message dest){
		if (src == null){
			return null;
		}
		
		CowBuffer cowBuffer = null;
		if (src.cowBuffer != null){
			cowBuffer = src.cowBuffer;
			src.cowBuffer = null;
		}
		
		if (dest == null){
			dest = new Message();
		}
		
		dest.sender = src.getSender();
		dest.type = src.getType();
		if (dest.cowBuffer != null){
			dest.cowBuffer.decrRef();
		}
		
		dest.cowBuffer = cowBuffer;
		dest.argsCopyFrom(src);
		dest.readIndex = 0;
		dest.readPos = 0;
		
		// 释放src
		src.release();
		return dest;
	}
	
	// 序列化所有的参数到buffer中，并返回；调用此方法后，消息本身已释放，已经不合法
	CowBuffer move(){
		if (argsIsEmpty()){
			release();
			return null;
		}
		
//		int argsLen = argsLength();
//		int length = isBufferEmpty() ? Stream.fixSizeOfInt32() : 0;
//		reserve(argsLen + length);
		// 将能序列化的对象序列化
		Stream stream = AdataUtils.getStream();
		writeArgs(stream);
		writeMeta(stream);
		
		CowBuffer buffer = null;
		if (cowBuffer != null){
			buffer = cowBuffer;
			cowBuffer = null;
		}
		
		release();
		return buffer;
	}
	
	@Override
	public void read(Stream stream){
		clear();
		
		int argsLen = stream.fixReadInt32();
//		sender.read(stream);
//		type = Atom.from(stream.readInt64());
//		argsSize = stream.readInt32();

		if (argsLen > 0){
			int bufferSize = argsLen + Stream.fixSizeOfInt32();
			reserve(bufferSize);
//			if (cowBuffer == null){
//				cowBuffer = CowBufferPool.borrowObject(length);
//			}else{
//				CowBuffer newBuffer = cowBuffer.reserve(length);
//				if (newBuffer != cowBuffer){
//					cowBuffer.decrRef();
//					cowBuffer = newBuffer;
//				}else if (cowBuffer.getRefCount() > 1){
//					cowBuffer = cowBuffer.copyOnWrite();
//				}
//			}
			byte[] srcBuffer = stream.getReadBuffer();
			CowBuffer.copy(srcBuffer, cowBuffer, stream.readLength() - Stream.fixSizeOfInt32(), bufferSize);
			stream.skipRead(argsLen);
		}
		
		sender.read(stream);
		type = Atom.from(stream.readInt64());
		argsSize = stream.readInt32();
	}
	
	@Override
	public void write(Stream stream){
		if (argsIsEmpty()){
			return;
		}

		int argsLen = argsLength();
		int startPos = stream.writeLength();
		int bufferSize = 0;
		boolean startBufferEmpty = isBufferEmpty();
		if (!startBufferEmpty){
			// 读出当前cowBuffer的参数长度值
			byte[] oldReadBuffer = stream.getReadBuffer();
			int oldReadLen = stream.readLength();
			stream.setReadBuffer(cowBuffer.getBuffer());
			stream.clearRead();
			bufferSize = stream.fixReadInt32();
			stream.setReadBuffer(oldReadBuffer);
			stream.clearRead();
			stream.skipRead(oldReadLen);
			
			// 拷贝已经序列化的部分
			byte[] dest = stream.getWriteBuffer();
			System.arraycopy(cowBuffer.getBuffer(), 0, dest, startPos, bufferSize + Stream.fixSizeOfInt32());
			stream.skipWrite(bufferSize + Stream.fixSizeOfInt32());
		}
		
		if (startBufferEmpty){
			stream.fixWriteInt32(argsLen);
//			cowBuffer.write(Stream.fixSizeOfInt32());
		}
		
		if (argsLen > 0){
			// 将能序列化的对象序列化
//			writeArgs(argsLen, stream);
			for (int i=writeIndex; i<argsSize(); ++i){
				Object arg = argsGet(i);
				writeArg(stream, arg);
			}
			if (!startBufferEmpty){
				int writeLen = stream.writeLength();
				stream.clearWrite();
				stream.skipWrite(startPos);
				stream.fixWriteInt32(argsLen + bufferSize);
				stream.clearWrite();
				stream.skipWrite(writeLen);
			}
		}
		
		// 将meta数据序列化
		writeMeta(stream);
	}
	
	/**
	 * fixLength(4) + argsLength(*) + sender(*)+type(*)+argsSize(*)
	 * @return
	 */
	@Override
	public int sizeOf(){
		return Stream.fixSizeOfInt32() + argsLength() + metaLength();
	}
	
	private boolean isBufferEmpty(){
		return cowBuffer == null || cowBuffer.size() == 0;
	}
	
	private void copyOnWrite(){
		if (cowBuffer != null && cowBuffer.getRefCount() > 1){
//			int argsLen = argsLength();
//			int length = isBufferEmpty() ? Stream.fixSizeOfInt32() : 0;
//			reserve(argsLen + length);
			// 将能序列化的对象序列化
			writeArgs(AdataUtils.getStream());
		}
	}
	
	private void reserve(int grownLen){
		if (cowBuffer == null){
			cowBuffer = CowBufferPool.borrowObject(grownLen);
		}else{
			CowBuffer newBuffer = cowBuffer.reserve(grownLen);
			if (newBuffer != cowBuffer){
				cowBuffer.decrRef();
				cowBuffer = newBuffer;
			}else if (cowBuffer.getRefCount() > 1){
				cowBuffer = cowBuffer.copyOnWrite();
			}
		}
	}
	
	private int metaLength(){
		int length = 0;
		length += sender.sizeOf();
		length += Stream.sizeOfInt64(Atom.to(type));
		length += Stream.sizeOfInt32(argsSize);
		return length;
	}
	
	/**
	 * 需要序列化的参数的长度
	 * @return
	 */
	private int argsLength(){
		int length = 0;
		for (int i=writeIndex; i<argsSize(); ++i){
			Object arg = argsGet(i);
			if (arg != null){
				if (arg instanceof Base){
					length += ((Base) arg).sizeOf();
				}else if (arg instanceof IAdlAdapter){
					length += ((IAdlAdapter) arg).sizeOf();
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
	
	private void writeArgs(Stream stream){
		int argsLen = argsLength();
		int length = isBufferEmpty() ? Stream.fixSizeOfInt32() : 0;
		reserve(argsLen + length);
		if (argsLen == 0){
			return;
		}
		
//		Stream stream = AdataUtils.getStream();
		
		stream.clearWrite();
		stream.setWriteBuffer(cowBuffer.getBuffer());
		int bufferSize = cowBuffer.size();
		boolean startBufferEmpty = isBufferEmpty();
		if (!startBufferEmpty){
			// 读出当前cowBuffer的参数长度值
			byte[] oldReadBuffer = stream.getReadBuffer();
			int oldReadLen = stream.readLength();
			stream.setReadBuffer(cowBuffer.getBuffer());
			stream.clearRead();
			bufferSize = stream.fixReadInt32();
			stream.setReadBuffer(oldReadBuffer);
			stream.clearRead();
			stream.skipRead(oldReadLen);
		}
		
		if (startBufferEmpty){
			stream.fixWriteInt32(argsLen);
			cowBuffer.write(Stream.fixSizeOfInt32());
		}
		
		// 将能序列化的对象序列化
		int writeLen = stream.writeLength();
//		writeArgs(argsLen, stream);
		for (int i=writeIndex; i<argsSize(); ++i, ++writeIndex){
			Object arg = argsGet(i);
			if (writeArg(stream, arg)){
				argsSet(i, null);
			}
		}
		cowBuffer.write(stream.writeLength() - writeLen);
		if (!startBufferEmpty){
			writeLen = stream.writeLength();
			stream.clearWrite();
			stream.fixWriteInt32(argsLen + bufferSize);
			stream.clearWrite();
			stream.skipWrite(writeLen);
		}

//		stream.fixWriteInt32(length);
//		if (isBufferEmpty()){
//			cowBuffer.write(Stream.fixSizeOfInt32());
//		}
//		stream.clearWrite();
//		stream.skipWrite(cowBuffer.size());
//		
//		try{
//			// 将能序列化的对象序列化
//			writeArgs(length, stream);
//		}finally{
//			stream.setWriteBuffer(null);
//		}
	}
	
//	private void writeArgs(int length, Stream stream){
//		for (int i=writeIndex; i<argsSize(); ++i, ++writeIndex){
//			Object arg = argsGet(i);
//			if (writeArg(stream, arg)){
//				argsSet(i, null);
//			}
//		}
//	}
	
	private void writeMeta(Stream stream){
		writeArg(stream, sender);
		writeArg(stream, Atom.to(type));
		writeArg(stream, argsSize);
	}
	
	private boolean writeArg(Stream stream, Object arg){
		if (arg == null){
			return true;
		}
		
		boolean isAdata = true;
//		int size = cowBuffer.size();
		if (arg instanceof Base){
			Base o = (Base) arg;
			o.write(stream);
		}else if (arg instanceof IAdlAdapter){
			IAdlAdapter adat = (IAdlAdapter) arg;
			adat.write(stream);
		}else if (arg instanceof Byte){
			Byte o = (Byte) arg;
			stream.writeInt8(o);
		}else if (arg instanceof Boolean){
			Byte o = (byte) ((boolean) arg ? 1 : 0);
			stream.writeInt8(o);
		}else if (arg instanceof Short){
			Short o = (Short) arg;
			stream.writeInt16(o);
		}else if (arg instanceof Integer){
			Integer o = (Integer) arg;
			stream.writeInt32(o);
		}else if (arg instanceof Long){
			Long o = (Long) arg;
			stream.writeInt64(o);
		}else if (arg instanceof Float){
			Float o = (Float) arg;
			stream.writeFloat(o);
		}else if (arg instanceof Double){
			Double o = (Double) arg;
			stream.writeDouble(o);
		}else if (arg instanceof String){
			String o = (String) arg;
			stream.writeString(o);
		}else{
			isAdata = false;
		}
//		cowBuffer.write(stream.writeLength() - size);
		return isAdata;
	}
	
	// 用户自定义消息可以实现下面两个方法
	protected void initMsg(){
	}
	
	protected void resetMsg(){
	}
	
	/** 以下继承自AbstractNode */
	@Override
	protected final void initNode(){
		initMsg();
	}
	
	@Override
	protected final void resetNode(){
		resetMsg();
		super.clear();
		writeIndex = 0;
		
		totalNext = null;
		totalPrev = null;
		typeNext = null;
		typePrev = null;
		typeSameNext = null;
		typeSamePrev = null;

		senderTotalNext = null;
		senderTotalPrev = null;
		senderTypeNext = null;
		senderTypePrev = null;
		senderTypeSameNext = null;
		senderTypeSamePrev = null;
	}

	@Override
	public void release(){
		if (recycler != null){
			if (cowBuffer != null){
				cowBuffer.decrRef();
				cowBuffer = null;
			}
			recycler.returnObject(this);
		}
	}

//	@Override
//	public INode getNext(){
//		return next;
//	}
//
//	@Override
//	public INode fetchNext(){
//		INode nx = next;
//		next = null;
//		return nx;
//	}

//	@Override
//	public void onReturn(){
//		super.clear();
//		writeIndex = 0;
//		next = null;
//		recycler = null;
//		
//		totalNext = null;
//		totalPrev = null;
//		typeNext = null;
//		typePrev = null;
//		typeSameNext = null;
//		typeSamePrev = null;
//
//		senderTotalNext = null;
//		senderTotalPrev = null;
//		senderTypeNext = null;
//		senderTypePrev = null;
//		senderTypeSameNext = null;
//		senderTypeSamePrev = null;
//	}

//	@Override
//	public void onBorrowed(IRecycler recycler){
//		this.recycler = recycler;
//		this.next = null;
//	}

//	@Override
//	public void setNext(INode next){
//		this.next = next;
//	}

	
	/** 以下实现IMail接口 */
	private IMail totalNext;
	private IMail totalPrev;
	private IMail typeNext;
	private IMail typePrev;
	private IMail typeSameNext;
	private IMail typeSamePrev;

	private IMail senderTotalNext;
	private IMail senderTotalPrev;
	private IMail senderTypeNext;
	private IMail senderTypePrev;
	private IMail senderTypeSameNext;
	private IMail senderTypeSamePrev;
	
	@Override
	public void setTotalNext(IMail next) {
		totalNext = next;
	}

	@Override
	public IMail getTotalNext() {
		return totalNext;
	}

	@Override
	public void setTotalPrev(IMail prev) {
		totalPrev = prev;
	}

	@Override
	public IMail getTotalPrev() {
		return totalPrev;
	}

	@Override
	public void setTypeNext(IMail next) {
		typeNext = next;
	}

	@Override
	public IMail getTypeNext() {
		return typeNext;
	}

	@Override
	public void setTypePrev(IMail prev) {
		typePrev = prev;
	}

	@Override
	public IMail getTypePrev() {
		return typePrev;
	}

	@Override
	public void setTypeSameNext(IMail next) {
		typeSameNext = next;
	}

	@Override
	public IMail getTypeSameNext() {
		return typeSameNext;
	}

	@Override
	public void setTypeSamePrev(IMail prev) {
		typeSamePrev = prev;
	}

	@Override
	public IMail getTypeSamePrev() {
		return typeSamePrev;
	}

	@Override
	public void onClear() {
		this.release();
	}

	@Override
	public void setSenderTotalNext(IMail next) {
		senderTotalNext = next;
	}

	@Override
	public IMail getSenderTotalNext() {
		return senderTotalNext;
	}

	@Override
	public void setSenderTotalPrev(IMail prev) {
		senderTotalPrev = prev;
	}

	@Override
	public IMail getSenderTotalPrev() {
		return senderTotalPrev;
	}

	@Override
	public void setSenderTypeNext(IMail next) {
		senderTypeNext = next;
	}

	@Override
	public IMail getSenderTypeNext() {
		return senderTypeNext;
	}

	@Override
	public void setSenderTypePrev(IMail prev) {
		senderTypePrev = prev;
	}

	@Override
	public IMail getSenderTypePrev() {
		return senderTypePrev;
	}

	@Override
	public void setSenderTypeSameNext(IMail next) {
		senderTypeSameNext = next;
	}

	@Override
	public IMail getSenderTypeSameNext() {
		return senderTypeSameNext;
	}

	@Override
	public void setSenderTypeSamePrev(IMail prev) {
		senderTypeSamePrev = prev;
	}

	@Override
	public IMail getSenderTypeSamePrev() {
		return senderTypeSamePrev;
	}
}
