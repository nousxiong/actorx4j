/**
 * 
 */
package actorx.util;

//import org.apache.mina.core.buffer.IoBuffer;

//import actorx.adl.IAdlAdapter;
//import adata.Base;
import adata.Stream;

/**
 * @author Xiong
 *
 */
public final class AdataUtils {
	private static ThreadLocal<Stream> streamTLS = new ThreadLocal<Stream>();
	
	/**
	 * 获取局部存储的adata Stream
	 * @return
	 */
	public static Stream getStream(){
		Stream stream = streamTLS.get();
		if (stream == null){
			stream = new Stream();
			streamTLS.set(stream);
		}
		return stream;
	}

//	public static <T extends Base> void readAdl(T t, IoBuffer ioBuffer) {
//		byte[] bytes = ioBuffer.array();
//		Stream stream = getStream();
//		stream.setReadBuffer(bytes);
//		int start = ioBuffer.position();
//		stream.skipRead(start);
//		t.read(stream);
//		ioBuffer.skip(stream.readLength() - start);
//	}
//
//	public static <T extends IAdlAdapter> void read(T t, IoBuffer ioBuffer) {
//		byte[] bytes = ioBuffer.array();
//		Stream stream = getStream();
//		stream.setReadBuffer(bytes);
//		int start = ioBuffer.position();
//		stream.skipRead(start);
//		t.read(stream);
//		ioBuffer.skip(stream.readLength() - start);
//	}
//
//	public static <T extends Base> void writeAdl(T t, IoBuffer ioBuffer) {
//		int size = t.sizeOf();
//		if (size > ioBuffer.remaining()){
//			ioBuffer.expand(size);
//		}
//		byte[] bytes = ioBuffer.array();
//		Stream stream = AdataUtils.getStream();
//		stream.setWriteBuffer(bytes);
//		int start = ioBuffer.position();
//		stream.skipWrite(start);
//		t.write(stream);
//		ioBuffer.skip(stream.writeLength() - start);
//	}
//
//	public static <T extends IAdlAdapter> void write(T t, IoBuffer ioBuffer) {
//		int size = t.sizeOf();
//		if (size > ioBuffer.remaining()){
//			ioBuffer.expand(size);
//		}
//		byte[] bytes = ioBuffer.array();
//		Stream stream = AdataUtils.getStream();
//		stream.setWriteBuffer(bytes);
//		int start = ioBuffer.position();
//		stream.skipWrite(start);
//		t.write(stream);
//		ioBuffer.skip(stream.writeLength() - start);
//	}
	
}
