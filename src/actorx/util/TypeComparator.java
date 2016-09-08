/**
 * 
 */
package actorx.util;

/**
 * @author Xiong
 * 消息类型比较器
 */
public class TypeComparator {
	
	/**
	 * 比较2个Type
	 * @param lhs
	 * @param rhs
	 * @return 0 ==, -1 <, 1 >
	 */
	public static int compare(String lhs, String rhs){
		if (lhs != null && rhs == null){
			return 1;
		}else if (lhs == null && rhs != null){
			return -1;
		}else if (lhs == null && rhs == null){
			return 0;
		}
		
		int r = lhs.compareTo(rhs);
		if (r > 0){
			return 1;
		}else if (r < 0){
			return -1;
		}
		
		return 0;
	}
}
