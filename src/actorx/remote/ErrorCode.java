/**
 * 
 */
package actorx.remote;

/**
 * @author Xiong
 *
 */
public enum ErrorCode {
	SUCCESS(0),
	FAILURE(1),
	;
	
	// Error code
	private short code;
	private static final ErrorCode[] enums = values();
	
	public short getCode(){
		return code;
	}
	
	private ErrorCode(int code){
		this.code = (short) code;
	}
	
	/**
	 * 解析消息码
	 * @param code
	 * @return
	 */
	public static ErrorCode parse(int code){
		if (code >= 0 && code < enums.length){
			return enums[code];
		}
		throw new EnumConstantNotPresentException(ErrorCode.class, "Not match enum ErrorCode values "+code);
	}
}
