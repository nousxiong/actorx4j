/**
 * 
 */
package actorx.remote.mina;

import org.apache.mina.core.session.IoSession;

import cque.LinkedQueue;
import cque.Node;
import cque.SingleObjectPool;
import actorx.remote.NetSession;
import actorx.remote.mina.io.request.MinaRequestMessage;

/**
 * @author Xiong
 *
 */
public class MinaSession extends NetSession {
	private IoSession session;
	private LinkedQueue<MinaRequestMessage> reqQue;
	
	public MinaSession(SingleObjectPool<Node<MinaRequestMessage>> requsetPool){
		reqQue = new LinkedQueue<MinaRequestMessage>(requsetPool);
	}
	
	public IoSession getSession() {
		return session;
	}
	
	public void setSession(IoSession session) {
		this.session = session;
	}

	public LinkedQueue<MinaRequestMessage> getRequestQueue() {
		return reqQue;
	}

	@Override
	public void close() {
		if (session != null){
			session.closeNow();
		}
	}
	
}
