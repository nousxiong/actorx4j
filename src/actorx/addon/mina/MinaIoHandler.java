/**
 * 
 */
package actorx.addon.mina;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import actorx.Actor;
import actorx.ActorAddon;

/**
 * @author Xiong
 *
 */
public class MinaIoHandler extends ActorAddon implements IoHandler {
	public MinaIoHandler(Actor hostAx){
		super(hostAx);
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		send(MinaAtomCode.EXCAUGHT, session, cause);
	}
	
	@Override
	public void inputClosed(IoSession session) throws Exception {
		session.closeNow();
		send(MinaAtomCode.ICLOSED, session);
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		send(MinaAtomCode.MRECVD, session, message);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		send(MinaAtomCode.MSENT, session, message);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		send(MinaAtomCode.SCLOSED, session);
	}
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		send(MinaAtomCode.SCREATED, session);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		send(MinaAtomCode.SIDLE, session, status);
	}
	
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		send(MinaAtomCode.SOPENED, session);
	}
}
