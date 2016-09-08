/**
 * 
 */
package actorx.util;

import cque.INode;
import cque.INodeFactory;

/**
 * @author Xiong
 * 
 */
public class SenderMailListFactory implements INodeFactory{

	@Override
	public INode createInstance() {
		return new SenderMailList();
	}

}
