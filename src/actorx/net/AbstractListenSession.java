/**
 * 
 */
package actorx.net;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author Xiong
 * @creation 2016年10月6日上午12:44:38
 *
 */
public abstract class AbstractListenSession {
	private AsynchronousServerSocketChannel acpr;
	
	public void setAcceptor(AsynchronousServerSocketChannel acpr){
		this.acpr = acpr;
	}
	
	public AsynchronousServerSocketChannel getAcceptor(){
		return acpr;
	}
	
	public void close(){
		try {
			acpr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract AbstractAioSession makeAioSession(AsynchronousSocketChannel skt);
}
