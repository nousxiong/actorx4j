/**
 * 
 */
package actorx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import actorx.util.Atom;
import actorx.util.DefaultCustomMessageFactory;
import cque.util.PoolUtils;

/**
 * @author Xiong
 *
 */
public class ActorSystemConfig {
	private long axid;
	private int threadNum = 0;
	private ExecutorService actorThreadPool;
	private ExecutorService networkThreadPool;
	private int internalPoolSize = PoolUtils.DEFAULT_POOL_SIZE;
	private int internalPoolInitSize = PoolUtils.DEFAULT_INIT_SIZE;
	private int internalPoolMaxSize = PoolUtils.DEFAULT_MAX_SIZE;
	private Map<String, ICustomMessageFactory> customMsgList = new HashMap<String, ICustomMessageFactory>();
	
	public long getAxid() {
		return axid;
	}
	
	public ActorSystemConfig setAxid(String axid) {
		this.axid = Atom.to(axid);
		return this;
	}
	
	public ActorSystemConfig setAxid(long axid){
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
	
	public ActorSystemConfig addCustomMessage(String type, ICustomMessageFactory factory){
		if (customMsgList.put(type, factory) != null){
			throw new RuntimeException("Custom message<"+type+"> already existed");
		}
		return this;
	}
	
	public ActorSystemConfig addCustomMessage(String type, Class<? extends AbstractCustomMessage> clazz){
		if (customMsgList.put(type, new DefaultCustomMessageFactory(clazz)) != null){
			throw new RuntimeException("Custom message<"+type+"> already existed");
		}
		return this;
	}
	
	public Map<String, ICustomMessageFactory> getCustomMessageList(){
		return customMsgList;
	}
	
}
