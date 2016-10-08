/**
 * 
 */
package actorx.net;

import java.net.InetSocketAddress;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:46:55
 *
 */
public class ConnectOptions {
	public InetSocketAddress inetSocketAddress;
	public SocketOptions socketOptions = new SocketOptions();
	
	public ConnectOptions inetSocketAddress(InetSocketAddress inetSocketAddress){
		this.inetSocketAddress = inetSocketAddress;
		return this;
	}
	
	public ConnectOptions socketOptions(SocketOptions socketOptions){
		this.socketOptions = socketOptions;
		return this;
	}
}
