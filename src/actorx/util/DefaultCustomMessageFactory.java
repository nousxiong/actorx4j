/**
 * 
 */
package actorx.util;

import actorx.AbstractCustomMessage;
import actorx.ICustomMessageFactory;

/**
 * @author Xiong
 *
 */
public class DefaultCustomMessageFactory implements ICustomMessageFactory {
	private Class<? extends AbstractCustomMessage> clazz;
	
	public DefaultCustomMessageFactory(Class<? extends AbstractCustomMessage> clazz){
		this.clazz = clazz;
	}

	@Override
	public AbstractCustomMessage createInstance() {
		try{
			return clazz.newInstance();
		}catch (InstantiationException | IllegalAccessException e){
			throw new RuntimeException(e);
		}
	}

}
