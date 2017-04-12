/**
 * 
 */
package actorx.util;

/**
 * @author Xiong
 * @creation 2017年2月16日下午6:23:03
 *
 */
public final class StringUtils {
	
	/**
	 * 是否为空
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str){
		return str == null || str.isEmpty();
	}
	
	/**
	 * 比较两个字符串
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static boolean equals(String lhs, String rhs){
		if (lhs == null && rhs == null){
			return true;
		}else if (lhs == null || rhs == null){
			return false;
		}
		
		return lhs.equals(rhs);
	}
	
}
