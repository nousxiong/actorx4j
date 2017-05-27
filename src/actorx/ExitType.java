/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * actor退出时的方式
 */
public enum ExitType {
	NORMAL(0), // 正常退出
	EXCEPT(1), // 异常退出
	ALREADY(2), // 链接时已经退出
	NETERR(3), // 网络错误
	;

	private byte value;
	private static final ExitType[] enums = values();
	
	public byte getValue(){
		return value;
	}
	
	public static ExitType parse(int value){
		if (value >= 0 && value < enums.length){
			return enums[value];
		}
		throw new EnumConstantNotPresentException(ExitType.class, "Not match enum ExitType's values");
	}
	
	private ExitType(int value){
		this.value = (byte) value;
	}
}
