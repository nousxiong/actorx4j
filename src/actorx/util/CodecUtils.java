/**
 * 
 */
package actorx.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Xiong
 *
 */
public class CodecUtils {
	private final static Charset charset = StandardCharsets.UTF_8;
	private final static ThreadLocal<CharsetEncoder> encoderTLS = new ThreadLocal<CharsetEncoder>();
	private final static ThreadLocal<CharsetDecoder> decoderTLS = new ThreadLocal<CharsetDecoder>();

	/**
	 * 获取上下文编码对象
	 * @return
	 */
	public static CharsetEncoder getEncoder(){
		CharsetEncoder enc = encoderTLS.get();
		if (enc == null){
			Charset cs = getCharset();
			enc = cs.newEncoder();
			encoderTLS.set(enc);
		}
		return enc;
	}
	
	/**
	 * 获取上下文解码对象
	 * @return
	 */
	public static CharsetDecoder getDecoder(){
		CharsetDecoder dec = decoderTLS.get();
		if (dec == null){
			Charset cs = getCharset();
			dec = cs.newDecoder();
			decoderTLS.set(dec);
		}
		return dec;
	}
	
	private static Charset getCharset(){
		return charset;
	}
}
