/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * actorx的默认消息类型
 */
public class MsgType {
	public static final String EXIT = "EXIT";
	public static final String NULLTYPE = "NULLTYPE";
	public static final String LINK = "LINK";
	public static final String MONITOR = "MONITOR";
	
	public static final String OPEN = "OPEN";
	public static final String ACCEPT_ERR = "ACCEPT_ERR";
	public static final String CONN_ERR = "CONN_ERR";
	public static final String RECV = "RECV";
	public static final String EXCEPT = "EXCEPT";
	public static final String CLOSE = "CLOSE";
	
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
