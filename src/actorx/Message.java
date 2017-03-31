/**
 * 
 */
package actorx;

import actorx.detail.CowBuffer;
import actorx.detail.IMail;
import actorx.detail.Pack;
import adata.Base;
import adata.Stream;
import cque.INode;
import cque.IRecycler;

/**
 * @author Xiong
 */
public class Message extends Pack implements INode, IMail {
	// 空消息
	public static final Message NULLMSG = new Message();
	
	private CowBuffer cowBuffer;
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
		
		if (!src.argsIsEmpty() && src.cowBuffer == null){
			src.cowBuffer = CowBufferPool.borrowObject();
		}
		
		if (src.cowBuffer != null){
			// 将src所有参数序列化后再做共享
			src.reserve();
			src.writeArgs();
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
	 * 获取指定类型的参数
	 * @return
	 */
	public <T extends Base> T get(T t){
		return super.get(cowBuffer, t);
	}
	
	/**
	 * 获取指定类型并可能使用Class来创建新实例的参数
	 * @param c
	 * @return
	 */
	public <T extends Base> T get(Class<T> c){
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
	 * 转移相关数据到新Packet中
	 * @note 调用此方法后，消息本身需要释放，已经不合法
	 * @return
	 */
	Packet move(Packet pkt){
		CowBuffer buffer = null;
		if (cowBuffer != null){
			buffer = cowBuffer;
			cowBuffer = null;
		}
		
		if (pkt == null){
			pkt = new Packet();
		}
		pkt.set(this, buffer);
		return pkt;
	}
	
	// 序列化所有的参数到buffer中，并返回；调用此方法后，消息本身需要释放，已经不合法
	CowBuffer move(){
		if (argsIsEmpty()){
			return null;
		}
		
		int length = argsLength();
		if (cowBuffer == null){
			cowBuffer = CowBufferPool.borrowObject(length);
		}else{
			CowBuffer newBuffer = cowBuffer.reserve(length);
			if (newBuffer != cowBuffer){
				cowBuffer.decrRef();
				cowBuffer = newBuffer;
			}else if (cowBuffer.getRefCount() > 1){
				cowBuffer = cowBuffer.copyOnWrite();
			}
		}
		// 将能序列化的对象序列化
		writeArgs();
		
		CowBuffer buffer = null;
		if (cowBuffer != null){
			buffer = cowBuffer;
			cowBuffer = null;
		}
		return buffer;
	}
	
	private void copyOnWrite(){
		if (cowBuffer != null && cowBuffer.getRefCount() > 1){
			if (!reserve()){
				// 写时拷贝，重新创建一个新buffer，拷贝旧数据，decr旧buffer引用计数
				cowBuffer = cowBuffer.copyOnWrite();
			}
			// 将能序列化的对象序列化
			writeArgs();
		}
	}
	
	private boolean reserve(){
		int length = argsLength();
		CowBuffer newBuffer = cowBuffer.reserve(length);
		if (newBuffer != cowBuffer){
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
		int length = 0;
		for (int i=writeIndex; i<argsSize(); ++i){
			Object arg = argsGet(i);
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
		for (int i=writeIndex; i<argsSize(); ++i, ++writeIndex){
			Object arg = argsGet(i);
			if (writeArg(arg)){
				argsSet(i, null);
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
			byte[] buffer = cowBuffer.getBuffer();
			int size = cowBuffer.size();
			stream.clearWrite();
			stream.setWriteBuffer(buffer);
			stream.skipWrite(size);
			if (arg instanceof Base){
				Base o = (Base) arg;
				o.write(stream);
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
			cowBuffer.write(stream.writeLength() - size);
		}finally{
			stream.setWriteBuffer(null);
		}
		return isAdata;
	}
	
	
	/** 以下实现INode接口 */
	private INode next;
	private IRecycler recycler;

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
	public void onReturn(){
		super.clear();
		writeIndex = 0;
		next = null;
		recycler = null;
		
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
	public void onBorrowed(IRecycler recycler){
		this.recycler = recycler;
		this.next = null;
	}

	@Override
	public void setNext(INode next){
		this.next = next;
	}

	
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
