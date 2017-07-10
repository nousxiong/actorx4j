/**
 * 
 */
package actorx.remote;

import cque.util.PoolUtils;

/**
 * @author Xiong
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class NetworkInfo<T extends NetworkInfo> {
	// 通用
	private ProtocolType ptype;
	private String host;
	private int port = -1;
	private int heartTimeout = 7; // 心跳超时时间，单位秒
	
	// Mina
	private int minaIdleTime = 15; // 建议大于{@link NetworkInfo#heartTimeout}其两倍
	private int minaMsgPoolSize = PoolUtils.DEFAULT_POOL_SIZE;
	private int minaMsgPoolInitSize = PoolUtils.DEFAULT_INIT_SIZE;
	private int minaMsgPoolMaxSize = PoolUtils.DEFAULT_MAX_SIZE;

	public void parse(ProtocolType ptype, String remoteAddress){
		if (!ptype.check(remoteAddress)){
			throw new NetworkAddressException(ptype, remoteAddress);
		}
		
		int hostIndex = remoteAddress.indexOf("://");
		if (hostIndex == -1){
			throw new NetworkAddressException(remoteAddress);
		}
		hostIndex += 3;
		
		int portIndex = remoteAddress.indexOf(":", hostIndex);
		int slashIndex = remoteAddress.indexOf("/", hostIndex);
		if (slashIndex == -1){
			slashIndex = remoteAddress.length();
		}
		if (portIndex == -1){
			portIndex = slashIndex;
		}
		
		host = remoteAddress.substring(hostIndex, portIndex);
		if (portIndex != slashIndex){
			port = Integer.valueOf(remoteAddress.substring(portIndex + 1, slashIndex));
		}
		this.ptype = ptype;
	}

	public ProtocolType getProtocolType() {
		return ptype;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getHeartTimeout() {
		return heartTimeout;
	}

	public T setHeartTimeout(int heartTimeout) {
		this.heartTimeout = heartTimeout;
		return (T) this;
	}

	public int getMinaIdleTime() {
		return minaIdleTime;
	}

	public T setMinaIdleTime(int minaIdleTime) {
		this.minaIdleTime = minaIdleTime;
		return (T) this;
	}

	public int getMinaMsgPoolSize() {
		return minaMsgPoolSize;
	}

	public T setMinaMsgPoolSize(int minaMsgPoolSize) {
		this.minaMsgPoolSize = minaMsgPoolSize;
		return (T) this;
	}

	public int getMinaMsgPoolInitSize() {
		return minaMsgPoolInitSize;
	}

	public T setMinaMsgPoolInitSize(int minaMsgPoolInitSize) {
		this.minaMsgPoolInitSize = minaMsgPoolInitSize;
		return (T) this;
	}

	public int getMinaMsgPoolMaxSize() {
		return minaMsgPoolMaxSize;
	}

	public T setMinaMsgPoolMaxSize(int minaMsgPoolMaxSize) {
		this.minaMsgPoolMaxSize = minaMsgPoolMaxSize;
		return (T) this;
	}
}
