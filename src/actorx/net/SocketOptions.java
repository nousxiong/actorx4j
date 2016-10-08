/**
 * 
 */
package actorx.net;

import java.net.SocketOption;
import java.net.StandardSocketOptions;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:45:53
 *
 */
public class SocketOptions {
	public boolean soReuseAddr = true;
	public boolean tcpNodelay = true;
	public int soLinger = 0;
	public int soRcvBuf = 64 * 1024;
	public int soSndBuf = 64 * 1024;
	
	public <T> SocketOptions setOption(SocketOption<T> name, T value){
		if (name == StandardSocketOptions.SO_REUSEADDR){
			soReuseAddr = (Boolean) value;
		}else if (name == StandardSocketOptions.TCP_NODELAY){
			tcpNodelay = (Boolean) value;
		}else if (name == StandardSocketOptions.SO_LINGER){
			soLinger = (Integer) value;
		}else if (name == StandardSocketOptions.SO_RCVBUF){
			soRcvBuf = (Integer) value;
		}else if (name == StandardSocketOptions.SO_SNDBUF){
			soSndBuf = (Integer) value;
		}
		return this;
	}
}
