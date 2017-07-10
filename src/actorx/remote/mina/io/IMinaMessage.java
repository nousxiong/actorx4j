/**
 * 
 */
package actorx.remote.mina.io;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * @author Xiong
 * @creation 2017年1月28日下午9:14:34
 *
 */
public interface IMinaMessage {
	// size + code
	public static final int HEADER_LENGTH = 4;
	
	MinaMsgCode getMsgCode();
	void decode(IoBuffer ioBuffer);
	IoBuffer encode();
	String toLogString();
}
