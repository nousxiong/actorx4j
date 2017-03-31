/**
 * 
 */
package actorx.detail;

import actorx.Message;
import cque.IObjectFactory;
import cque.IPooledObject;

/**
 * @author Xiong
 *
 */
public class MessageFactory implements IObjectFactory {
	@Override
	public IPooledObject createInstance() {
		return new Message();
	}
}
