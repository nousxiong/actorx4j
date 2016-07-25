/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import actorx.util.CopyOnWriteBuffer;

/**
 * @author Xiong
 * 测试写时拷贝Buffer
 */
public class CopyOnWriteBufferBase {

	@Test
	public void test() {
		CopyOnWriteBuffer cowBuffer = new CopyOnWriteBuffer();
		cowBuffer.incrRef();
		
		String str1 = "string1";
		String str2 = "data2";
		byte[] buf1 = new byte[32];
		Arrays.fill(buf1, (byte) 1);
		byte[] buf2 = new byte[32];
		Arrays.fill(buf2, (byte) 2);
		
		// Write
		cowBuffer.write(str1.getBytes(StandardCharsets.US_ASCII));
		cowBuffer.write(str2.getBytes(StandardCharsets.US_ASCII));
		cowBuffer.write(buf1);
		cowBuffer.write(buf2);
		
		// Read
		byte[] bytes = new byte[128];
		
		int readSize = 0;
		assertTrue(cowBuffer.read(bytes, 0, readSize, str1.length()) == str1.length());
		assertTrue(str1.equals(new String(bytes, 0, str1.length())));
		readSize += str1.length();
		
		assertTrue(cowBuffer.read(bytes, 0, readSize, str2.length()) == str2.length());
		assertTrue(str2.equals(new String(bytes, 0, str2.length())));
		readSize += str2.length();
		
		byte[] readBuf1 = new byte[buf1.length];
		assertTrue(cowBuffer.read(readBuf1, readSize) == buf1.length);
		assertTrue(Arrays.equals(buf1, readBuf1));
		readSize += buf1.length;
		
		byte[] readBuf2 = new byte[buf2.length];
		assertTrue(cowBuffer.read(readBuf2, readSize) == buf2.length);
		assertTrue(Arrays.equals(buf2, readBuf2));
		readSize += buf2.length;
		
		// Ref
		assertTrue(cowBuffer.getRefCount() == 1);
		assertTrue(cowBuffer.decrRef() == 0);
	}

}
