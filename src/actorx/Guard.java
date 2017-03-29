/**
 * 
 */
package actorx;

import cque.AbstractNode;
import cque.INode;

/**
 * @author Xiong
 * 对象守护者
 */
public class Guard extends AbstractNode implements AutoCloseable {
	private INode object = null;

	<T extends INode> Guard wrap(INode object){
		this.object = object;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends INode> T get(){
		return (T) object;
	}
	
	@Override
	public void close() throws Exception {
		if (object != null){
			object.release();
			object = null;
		}
		release();
	}
}
