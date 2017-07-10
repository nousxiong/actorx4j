/**
 * 
 */
package actorx.remote.mina.io;

/**
 * @author Xiong
 * @creation 2017年2月10日下午7:05:03
 *
 */
public enum MinaMsgCode {
	HEART_REQ(0),
	HEART_RESP(1),
	LINK_REQ(2),
	LINK_RESP(3),
	MONITOR_REQ(4),
	MONITOR_RESP(5),
	
	EXIT_PUSH(6),
	SEND_PUSH(7),
	;
	
	// Msg code
	private int code;
	private static final MinaMsgCode[] enums = values();
	
	public int getCode(){
		return code;
	}
	
	private MinaMsgCode(int code){
		this.code = code;
	}
	
	/**
	 * 解析消息码
	 * @param code
	 * @return
	 */
	public static MinaMsgCode parse(int code){
		if (code >= 0 && code < enums.length){
			return enums[code];
		}
		throw new EnumConstantNotPresentException(MinaMsgCode.class, "Not match enum MsgCode values "+code);
	}
}
