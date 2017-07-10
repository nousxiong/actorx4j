/**
 * 
 */
package actorx.remote;

/**
 * @author Xiong
 *
 */
public enum ProtocolType {
	MINA_TCP(0),
	MINA_UDP(1),
	JETTY_HTTP(2),
	;
	
	public int getValue(){
		return value;
	}
	
	public static ProtocolType parse(int value){
		if (value >= 0 && value < enums.length){
			return enums[value];
		}
		throw new EnumConstantNotPresentException(ProtocolType.class, "Not match enum ProtocolType values");
	}
	
	public boolean check(String address){
		switch (this){
		case MINA_TCP:
			if (!address.startsWith("tcp")){
				return false;
			}
			break;
		case MINA_UDP:
			if (!address.startsWith("udp")){
				return false;
			}
			break;
		case JETTY_HTTP:
			if (!address.startsWith("http")){
				return false;
			}
			break;
		}
		return true;
	}
	
	private ProtocolType(int value){
		this.value = value;
	}

	private int value;
	private static final ProtocolType[] enums = values();
}
