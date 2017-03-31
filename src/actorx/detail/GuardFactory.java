/**
 * 
 */
package actorx.detail;

import actorx.Guard;
import cque.IObjectFactory;
import cque.IPooledObject;

/**
 * @author Xiong
 * 守护者工厂
 */
public class GuardFactory implements IObjectFactory {
	
	@Override
	public IPooledObject createInstance() {
		return new Guard();
	}
	
}
