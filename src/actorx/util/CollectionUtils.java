/**
 * 
 */
package actorx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Xiong
 *
 */
public class CollectionUtils {
	
	/**
	 * 查看指定的容器是否为空（包括null）
	 * @param coll
	 * @return
	 */
	public static boolean isEmpty(Collection<?> coll){
		return coll == null || coll.isEmpty();
	}
	
	/**
	 * 拷贝src到dest中，如果dest是null创建后再拷贝；如果src是空，则清空dest
	 * @param src 可以是null
	 * @param dest 可以是null
	 * @return dest或者新创建的dest
	 */
	public static List<String> copyTypes(List<String> src, List<String> dest){
		if (isEmpty(src)){
			if (dest != null){
				dest.clear();
			}
		}else{
			if (dest == null){
				dest = new ArrayList<String>(src.size());
			}else{
				dest.clear();
			}
			dest.addAll(src);
		}
		return dest;
	}
	
	/**
	 * 同{@link CollectionUtils#copyTypes}
	 * @param src
	 * @param dest
	 * @return
	 */
	public static List<Object> copySenders(List<Object> src, List<Object> dest){
		if (isEmpty(src)){
			if (dest != null){
				dest.clear();
			}
		}else{
			if (dest == null){
				dest = new ArrayList<Object>(src.size());
			}else{
				dest.clear();
			}
			dest.addAll(src);
		}
		return dest;
	}
}
