/**
 * 
 */
package actorx.detail;

import actorx.Guard;
import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 * 消息守护者工厂
 */
public class MessageGuardFactory implements INodeFactory {
	
	@Override
	public INode createInstance() {
		return new Guard();
	}
	
}
