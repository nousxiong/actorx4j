/**
 * 
 */
package actorx;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Xiong
 */
public class ActorSystem {
	// ActorSystem unique id
	private long axid;
	/** 启动时间戳 */
	private long timestamp;
	/** actor全局表 */
	private Map<ActorId, Actor> actorMap = new ConcurrentHashMap<ActorId, Actor>();
	// ChannelGroup
	private AsynchronousChannelGroup chanGroup;
	/** Actor线程池 */
	private ExecutorService actorThreadPool;
	/** Channel线程池 */
	private ExecutorService chanThreadPool;
	private boolean outerThreadPool = false;
	/** ActorId分配基础 */
	private AtomicLong[] actorIdBases;
	private int currentIdBase = 0;
	
	
	public ActorSystem(String axid){
		this.axid = Atom.to(axid);
		this.timestamp = System.currentTimeMillis();
	}
	
	public void startup(){
		startup(Executors.newCachedThreadPool());
		outerThreadPool = false;
	}
	
	public void startup(int threadNum){
		outerThreadPool = false;
		this.actorIdBases = makeActorIdBases(threadNum);
		this.actorThreadPool = Executors.newFixedThreadPool(threadNum);
		this.chanThreadPool = Executors.newFixedThreadPool(threadNum);
		try{
			this.chanGroup = AsynchronousChannelGroup.withThreadPool(chanThreadPool);
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public void startup(ExecutorService threadPool){
		outerThreadPool = true;
		this.actorIdBases = makeActorIdBases(Runtime.getRuntime().availableProcessors());
		this.actorThreadPool = threadPool;
		this.chanThreadPool = threadPool;
		try{
			this.chanGroup = AsynchronousChannelGroup.withThreadPool(chanThreadPool);
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public void shutdown(){
		chanGroup.shutdown();
		if (!outerThreadPool && actorThreadPool != chanThreadPool){
			try{
				chanGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}catch (InterruptedException e){
			}
		}
		
		actorThreadPool.shutdown();
		if (!outerThreadPool){
			try{
				actorThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}catch (InterruptedException e){
			}
		}
		
		// 结束剩下的Actor
		for (Actor ax : actorMap.values()){
			ax.quit();
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
	 * 获取时间戳
	 * @return
	 */
	public long getTimestamp(){
		return timestamp;
	}
	
	/**
	 * 获取ChannelGroup
	 * @return
	 */
	public AsynchronousChannelGroup getChannelGroup(){
		return chanGroup;
	}
	
	/**
	 * 产生一个actor，自己调度
	 * @return 新Actor
	 */
	public Actor spawn(){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, null);
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
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	public ActorId spawn(IActorHandler ah){
		Actor actor = makeActor(ah);
		actorThreadPool.execute(actor);
		return actor.getActorId();
	}
	
	/**
	 * 产生一个actor，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param af 可运行单元
	 * @return 新Actor的Id
	 */
	public ActorId spawn(Actor sire, IActorHandler af){
		return spawn(sire, af, LinkType.NO_LINK);
	}

	/**
	 * 产生一个actor，指定和父Actor的链接关系，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param ah 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	public ActorId spawn(Actor sire, IActorHandler ah, LinkType link){
		Actor actor = makeActor(ah);
		addLink(sire, actor, link);
		actorThreadPool.execute(actor);
		return actor.getActorId();
	}
	
	/**
	 * 发送一个自己构建的消息
	 * @param toAid
	 * @param msg
	 */
	public void send(ActorId toAid, Message msg){
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送一个空消息
	 * @param fromAid
	 * @param toAid
	 */
	public void send(ActorId fromAid, ActorId toAid){
		Message msg = Message.make();
		
		msg.setSender(fromAid);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param fromAid
	 * @param toAid
	 * @param type
	 * @param arg
	 */
	public <A> void send(ActorId fromAid, ActorId toAid, String type, A arg){
		if (arg == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param fromAid
	 * @param toAid
	 * @param type
	 * @param arg
	 * @param arg1
	 */
	public <A, A1> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param fromAid
	 * @param toAid
	 * @param type
	 * @param arg
	 * @param arg1
	 * @param arg2
	 */
	public <A, A1, A2> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param fromAid
	 * @param toAid
	 * @param type
	 * @param args 可以为null
	 */
	public void send(ActorId fromAid, ActorId toAid, String type, Object... args){
		Message msg = Message.make();
		if (args != null){
			for (Object arg : args){
				if (arg == null){
					msg.release();
					throw new NullPointerException();
				}
				msg.put(arg);
			}
		}
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	Actor getActor(ActorId aid){
		return actorMap.get(aid);
	}
	
	boolean removeActor(ActorId aid){
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
	
	private Actor makeActor(IActorHandler ah){
		if (ah == null){
			throw new NullPointerException();
		}
		
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, ah);
		actorMap.put(aid, actor);
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
