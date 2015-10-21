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
public class Context {
	/** 单件 */
	private static Context instance = new Context();

	/** 启动时间戳 */
	private long timestamp;
	/** actor全局表 */
	private Map<ActorId, Actor> actorMap = new ConcurrentHashMap<ActorId, Actor>();
	/** Actor线程池 */
	private ExecutorService threadPool = null;
	/** ActorId分配基础 */
	private AtomicLong actorIdBase = new AtomicLong(0);
	
	public static Context getInstance(){
		return instance;
	}
	
	public Context(){
		this.timestamp = System.currentTimeMillis();
	}
	
	public void startup(){
		startup(Runtime.getRuntime().availableProcessors());
	}
	
	public void startup(int threadNum){
		this.threadPool = Executors.newFixedThreadPool(threadNum);
	}
	
	public void startup(ExecutorService threadPool){
		this.threadPool = threadPool;
	}
	
	public void join(){
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
	 * 产生一个actor，自己调度
	 * @return 新Actor
	 */
	public Actor spawn(){
		ActorId aid = generateActorId();
		Actor actor = new Actor(aid);
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
	
	public Actor getActor(ActorId aid){
		return actorMap.get(aid);
	}
	
	public void removeActor(ActorId aid){
		actorMap.remove(aid);
	}
	
	public ActorId generateActorId(){
		long id = actorIdBase.incrementAndGet();
		return new ActorId(id, timestamp);
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
		Actor actor = new Actor(aid);
		actorMap.put(aid, actor);
		af.setSelf(actor);
		return actor;
	}
}
