/**
 * 
 */
package actorx;

import java.util.concurrent.ExecutorService;

import cque.util.PoolUtils;

/**
 * @author Xiong
 *
 */
public class ActorSystemConfig {
	private String axid;
	private int threadNum = 0;
	private ExecutorService actorThreadPool;
	private ExecutorService networkThreadPool;
	private int internalPoolSize = PoolUtils.DEFAULT_POOL_SIZE;
	private int internalPoolInitSize = PoolUtils.DEFAULT_INIT_SIZE;
	private int internalPoolMaxSize = PoolUtils.DEFAULT_MAX_SIZE;
	
	public String getAxid() {
		return axid;
	}
	
	public ActorSystemConfig setAxid(String axid) {
		this.axid = axid;
		return this;
	}
	
	public int getThreadNum() {
		return threadNum;
	}
	
	public ActorSystemConfig setThreadNum(int threadNum) {
		this.threadNum = threadNum;
		return this;
	}
	
	public ExecutorService getActorThreadPool() {
		return actorThreadPool;
	}
	
	public ActorSystemConfig setActorThreadPool(ExecutorService actorThreadPool) {
		this.actorThreadPool = actorThreadPool;
		return this;
	}
	
	public ExecutorService getNetworkThreadPool() {
		return networkThreadPool;
	}
	
	public ActorSystemConfig setNetworkThreadPool(ExecutorService networkThreadPool) {
		this.networkThreadPool = networkThreadPool;
		return this;
	}
	
	public int getInternalPoolSize() {
		return internalPoolSize;
	}
	
	public ActorSystemConfig setInternalPoolSize(int internalPoolSize) {
		this.internalPoolSize = internalPoolSize;
		return this;
	}
	
	public int getInternalPoolInitSize() {
		return internalPoolInitSize;
	}
	
	public ActorSystemConfig setInternalPoolInitSize(int internalPoolInitSize) {
		this.internalPoolInitSize = internalPoolInitSize;
		return this;
	}
	
	public int getInternalPoolMaxSize() {
		return internalPoolMaxSize;
	}
	
	public ActorSystemConfig setInternalPoolMaxSize(int internalPoolMaxSize) {
		this.internalPoolMaxSize = internalPoolMaxSize;
		return this;
	}
	
	
}
