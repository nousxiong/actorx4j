/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * actorx的默认消息类型
 */
public class MessageType {
	public static final String EXIT = "EXIT";
	
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
	
	/**
	 * 比较2个Type是否相等
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static boolean equals(String lhs, String rhs){
		return compare(lhs, rhs) == 0;
	}
}
