/**
 * 
 */
package actorx.detail;

import cque.IObjectFactory;
import cque.IPooledObject;

/**
 * @author Xiong
 * 写时拷贝Buffer工厂
 */
public class CowBufferFactory implements IObjectFactory {
	private int capacity;
	public CowBufferFactory(int capacity){
		this.capacity = capacity;
	}
	
	@Override
	public IPooledObject createInstance() {
		return new CowBuffer(capacity);
	}

}
