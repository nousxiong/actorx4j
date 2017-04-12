/**
 * 
 */
package actorx.util;

import java.nio.charset.StandardCharsets;

/**
 * @author Xiong
 * 字符串和long互转
 */
public class Atom {
	
	/**
	 * 字符串转long
	 * @param str 64个合法ascii字符，见{@link Atom#decodingTable}
	 * @note 空格不能在首位
	 * @return
	 */
	public static long to(String str){
		byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
		if (bytes.length > 10){
			throw new RuntimeException("Only 10 characters are allowed for atom");
		}
		
		long value = 0;
		long encodeValue = 0;
		for (byte c : bytes){
			value *= decodingTable.length;
			encodeValue = encodingTable[c];
			value += encodeValue;
		}
		return value + odd;
	}
	
	/**
	 * long转字符串
	 * @param val
	 * @return
	 */
	public static String from(long val){
		byte[] buffer = getBuffer();
		int pos = 19;
		
		val -= odd;
		while (val != 0){
			buffer[pos--] = decodingTable[(int) (val % decodingTable.length)];
			val /= decodingTable.length;
		}
		++pos;
		byte[] bytes = getResultBuffer();
		System.arraycopy(buffer, pos, bytes, 0, 20 - pos);
		return new String(bytes, 0, 20 - pos, StandardCharsets.US_ASCII);
	}
	
	/**
	 * 比较两个atom值是否相等
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static boolean equals(String lhs, String rhs){
		if (lhs == null && rhs == null){
			return true;
		}else if (lhs == null || rhs == null){
			return false;
		}
		
		return to(lhs) == to(rhs);
	}
	
	/**
	 * 获取当前线程局部存储buffer
	 * @return
	 */
	private static byte[] getBuffer(){
		byte[] buffer = bufferTLS.get();
		if (buffer == null){
			buffer = new byte[bufferSize];
			bufferTLS.set(buffer);
		}
		return buffer;
	}
	
	/**
	 * 获取当前线程局部存储resultBuffer
	 * @return
	 */
	private static byte[] getResultBuffer(){
		byte[] resultBuffer = resultBufferTLS.get();
		if (resultBuffer == null){
			resultBuffer = new byte[bufferSize];
			resultBufferTLS.set(resultBuffer);
		}
		return resultBuffer;
	}

	private static final byte[] decodingTable = 
		" 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"
			.getBytes(StandardCharsets.US_ASCII);
	
	private static final byte[] encodingTable = new byte[128];
	static {
		for (byte i=0; i<decodingTable.length; ++i){
			encodingTable[decodingTable[i]] = i;
		}
	}
	
	private static final int bufferSize = 21;
	private static final long odd = 1L << 60;
	
	private static ThreadLocal<byte[]> bufferTLS = new ThreadLocal<byte[]>();
	private static ThreadLocal<byte[]> resultBufferTLS = new ThreadLocal<byte[]>();
}
