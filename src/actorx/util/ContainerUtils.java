/**
 * 
 */
package actorx.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author Xiong
 * @creation 2017年1月24日下午4:55:55
 *
 */
public class ContainerUtils {
	
	/**
	 * 查看指定的容器是否为空（包括null）
	 * @param coll
	 * @return
	 */
	public static boolean isEmpty(Collection<?> coll){
		return coll == null || coll.isEmpty();
	}
	
	/**
	 * 查看指定的map是否为空（包括null）
	 * @param map
	 * @return
	 */
	public static boolean isEmpty(Map<?, ?> map){
		return map == null || map.isEmpty();
	}
	
}
