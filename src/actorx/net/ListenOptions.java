/**
 * 
 */
package actorx.net;

import java.net.InetSocketAddress;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:46:13
 *
 */
public class ListenOptions {
	public InetSocketAddress inetSocketAddress = new InetSocketAddress(0);
	public int backlog = 100;
	public SocketOptions socketOptions = new SocketOptions();
	
	public ListenOptions backlog(int backlog){
		this.backlog = backlog;
		return this;
	}
	
	public ListenOptions inetSocketAddress(InetSocketAddress inetSocketAddress){
		this.inetSocketAddress = inetSocketAddress;
		return this;
	}
	
	public ListenOptions socketOptions(SocketOptions socketOptions){
		this.socketOptions = socketOptions;
		return this;
	}
}
