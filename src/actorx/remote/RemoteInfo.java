/**
 * 
 */
package actorx.remote;

/**
 * @author Xiong
 *
 */
public class RemoteInfo extends NetworkInfo<RemoteInfo> {
	// 通用
	private int responseTimeout = 2; // 回应信息超时时间，单位秒
	
	// Mina
	private int minaConnectTimeout = 5; // 连接超时时间，单位秒

	public RemoteInfo parseRemote(ProtocolType ptype, String remoteAddress){
		super.parse(ptype, remoteAddress);
		return this;
	}

	public int getResponseTimeout() {
		return responseTimeout;
	}

	public void setResponseTimeout(int responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public int getMinaConnectTimeout() {
		return minaConnectTimeout;
	}

	public RemoteInfo setMinaConnectTimeout(int connectTimeout) {
		this.minaConnectTimeout = connectTimeout;
		return this;
	}
}
