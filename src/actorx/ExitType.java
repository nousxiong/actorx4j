/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * actor退出时的方式
 */
public enum ExitType {
	NORMAL((byte) 0), // 正常退出
	EXCEPT((byte) 1), // 异常退出
	;
	
	public byte getValue(){
		return value;
	}
	
	public static ExitType parse(byte value){
		for (ExitType et : values()){
			if (et.getValue() == value){
				return et;
			}
		}
		throw new RuntimeException("Not match enum ExitType's values");
	}
	
	private ExitType(byte value){
		this.value = value;
	}

	private byte value;
}
