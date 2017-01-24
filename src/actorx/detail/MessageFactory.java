/**
 * 
 */
package actorx.detail;

import actorx.Message;
import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 *
 */
public class MessageFactory implements INodeFactory {
	
	public MessageFactory(int reserve){
		this.reserve = reserve;
	}
	
	@Override
	public INode createInstance() {
		return new Message(reserve);
	}
	
	private int reserve;
}
