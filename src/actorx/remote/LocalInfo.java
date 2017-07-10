/**
 * 
 */
package actorx.remote;

/**
 * @author Xiong
 *
 */
public class LocalInfo extends NetworkInfo<LocalInfo> {

	public LocalInfo parseLocal(ProtocolType ptype, String localAddress){
		super.parse(ptype, localAddress);
		return this;
	}
}
