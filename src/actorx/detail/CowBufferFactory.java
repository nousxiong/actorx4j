/**
 * 
 */
package actorx.detail;

import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 * 写时拷贝Buffer工厂
 */
public class CowBufferFactory implements INodeFactory {
	private int capacity;
	public CowBufferFactory(int capacity){
		this.capacity = capacity;
	}
	
	@Override
	public INode createInstance() {
		return new CowBuffer(capacity);
	}

}
