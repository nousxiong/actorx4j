/**
 * 
 */
package actorx.util;

import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 * 写时拷贝Buffer工厂
 */
public class CowBufferFactory implements INodeFactory {

	@Override
	public INode createInstance() {
		return new CopyOnWriteBuffer();
	}

}
