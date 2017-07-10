/**
 * 
 */
package actorx.remote.mina.io;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;

import actorx.Actor;
import actorx.ActorAddon;

/**
 * @author Xiong
 *
 */
public class MinaIoFutureListener extends ActorAddon implements IoFutureListener<ConnectFuture> {
	
	public MinaIoFutureListener(Actor hostAx){
		super(hostAx);
	}

	@Override
	public void operationComplete(ConnectFuture cfut) {
		if (!cfut.isConnected()){
			send("CONNERR", cfut.getException());
		}
	}

}
