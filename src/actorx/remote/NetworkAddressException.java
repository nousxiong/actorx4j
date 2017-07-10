/**
 * 
 */
package actorx.remote;

/**
 * @author Xiong
 *
 */
public class NetworkAddressException extends RuntimeException {
	private static final long serialVersionUID = 415797676312890847L;

	public NetworkAddressException(String remoteAddress){
		super("Remote address "+remoteAddress+" parse error");
	}
	
	public NetworkAddressException(ProtocolType ptype, String remoteAddress){
		super("Remote address "+remoteAddress+" parse error, not match protocol type "+ptype);
	}
}
