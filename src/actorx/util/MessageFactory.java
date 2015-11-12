/**
 * 
 */
package actorx.util;

import actorx.Message;
import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 *
 */
public class MessageFactory implements INodeFactory {
	@Override
	public INode createInstance() {
		return new Message();
	}
}
