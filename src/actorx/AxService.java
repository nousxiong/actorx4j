/**
 * 
 */
package actorx;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Xiong
 */
public class AxService {
	// AxService unique id
	private long axid;
	/** 启动时间戳 */
	private long timestamp;
	/** actor全局表 */
	private Map<ActorId, Actor> actorMap = new ConcurrentHashMap<ActorId, Actor>();
	/** Actor线程池 */
	private ExecutorService threadPool = null;
	/** ActorId分配基础 */
	private AtomicLong[] actorIdBases;
	private int currentIdBase = 0;
	
	
	public AxService(String axid){
		this.axid = Atom.to(axid);
		this.timestamp = System.currentTimeMillis();
	}
	
	public void startup(){
		startup(Executors.newCachedThreadPool());
	}
	
	public void startup(int threadNum){
		this.actorIdBases = makeActorIdBases(threadNum);
		this.threadPool = Executors.newFixedThreadPool(threadNum);
	}
	
	public void startup(ExecutorService threadPool){
		this.actorIdBases = makeActorIdBases(Runtime.getRuntime().availableProcessors());
		this.threadPool = threadPool;
	}
	
	public void shutdown(){
		threadPool.shutdown();
		try{
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}catch (InterruptedException e){
		}
		
		// 结束剩下的Actor
		for (Entry<ActorId, Actor> e : actorMap.entrySet()){
			e.getValue().quit();
		}
	}
	
	/**
	 * 获取axid
	 * @return
	 */
	public long getAxid(){
		return axid;
	}
	
	/**
	 * 产生一个actor，自己调度
	 * @return 新Actor
	 */
	public Actor spawn(){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, false);
		actorMap.put(aid, actor);
		return actor;
	}
	
	/**
	 * 产生一个actor，自己调度
	 * @param sire 父Actor
	 * @return 新Actor
	 */
	public Actor spawn(Actor sire){
		return spawn(sire, LinkType.NO_LINK);
	}
	
	/**
	 *  产生一个actor，指定和父Actor的链接关系，自己调度
	 * @param sire 父Actor
	 * @param link 链接关系类型
	 * @return 新Actor
	 */
	public Actor spawn(Actor sire, LinkType link){
		Actor actor = spawn();
		addLink(sire, actor, link);
		return actor;
	}
	
	/**
	 * 产生一个actor，并且使用指定的Handler来放入线程池执行
	 * @param af 可运行单元
	 * @return 新Actor的Id
	 */
	public ActorId spawn(AbstractHandler af){
		Actor actor = makeActor(af);
		threadPool.execute(af);
		return actor.getActorId();
	}
	
	/**
	 * 产生一个actor，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param af 可运行单元
	 * @return 新Actor的Id
	 */
	public ActorId spawn(Actor sire, AbstractHandler af){
		return spawn(sire, af, LinkType.NO_LINK);
	}

	/**
	 * 产生一个actor，指定和父Actor的链接关系，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param af 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	public ActorId spawn(Actor sire, AbstractHandler af, LinkType link){
		Actor actor = makeActor(af);
		addLink(sire, actor, link);
		threadPool.execute(af);
		return actor.getActorId();
	}

	public long getTimestamp(){
		return timestamp;
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	public Actor getActor(ActorId aid){
		return actorMap.get(aid);
	}
	
	public boolean removeActor(ActorId aid){
		return actorMap.remove(aid) != null;
	}
	
	private ActorId generateActorId(){
		int index = this.currentIdBase;
		long incr = actorIdBases[index].incrementAndGet();
		long id = incr * 100 + index;
		
		++index;
		if (index >= actorIdBases.length){
			index = 0;
		}
		this.currentIdBase = index;
		return new ActorId(axid, timestamp, id, id);
	}
	
	private static void addLink(Actor sire, Actor child, LinkType link){
		if (link == LinkType.LINKED){
			sire.addLink(child.getActorId());
			child.addLink(sire.getActorId());
		}else if (link == LinkType.MONITORED){
			child.addLink(sire.getActorId());
		}
	}
	
	private Actor makeActor(AbstractHandler af){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, true);
		actorMap.put(aid, actor);
		af.setSelf(actor);
		return actor;
	}
	
	private AtomicLong[] makeActorIdBases(int concurr){
		if (concurr < 0){
			concurr = 1;
		}else if (concurr > 99){
			concurr = 99;
		}
		
		AtomicLong[] actorIdBases = new AtomicLong[concurr];
		for (int i=0; i<actorIdBases.length; ++i){
			actorIdBases[i] = new AtomicLong(0);
		}
		return actorIdBases;
	}
}
