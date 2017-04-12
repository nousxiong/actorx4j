/**
 * 
 */
package actorx;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import actorx.util.Atom;
import actorx.util.ConcurrentHashMap;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

/**
 * @author Xiong
 */
public class ActorSystem {
	// ActorSystem unique id
	private long axid;
	/** 启动时间戳 */
	private long timestamp;
	/** actor全局表 */
	private ConcurrentHashMap<ActorId, Actor> actorMap = new ConcurrentHashMap<ActorId, Actor>();
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
		startup(Executors.newFixedThreadPool(threadNum));
		outerThreadPool = false;
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
	
	public void startup(ExecutorService actorThreadPool, ExecutorService chanThreadPool){
		outerThreadPool = true;
		this.actorIdBases = makeActorIdBases(Runtime.getRuntime().availableProcessors());
		this.actorThreadPool = actorThreadPool;
		this.chanThreadPool = chanThreadPool;
		try{
			this.chanGroup = AsynchronousChannelGroup.withThreadPool(chanThreadPool);
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	@Suspendable
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
	@Suspendable
	public Actor spawn(){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid);
		actorMap.put(aid, actor);
		return actor;
	}
	
	/**
	 * 产生一个actor，自己调度
	 * @param sire 父Actor
	 * @return 新Actor
	 */
	@Suspendable
	public Actor spawn(Actor sire){
		return spawn(sire, LinkType.NO_LINK);
	}
	
	/**
	 *  产生一个actor，指定和父Actor的链接关系，自己调度
	 * @param sire 父Actor
	 * @param link 链接关系类型
	 * @return 新Actor
	 */
	@Suspendable
	public Actor spawn(Actor sire, LinkType link){
		Actor actor = spawn();
		addLink(sire, actor, link);
		return actor;
	}
	
	/**
	 * 产生一个线程actor，并且使用指定的Handler来放入线程池执行
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(IThreadActorHandler ah){
		return spawn(actorThreadPool, ah);
	}
	
	/**
	 * 产生一个纤程actor，并且使用指定的Handler来放入默认的调度器执行
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(IFiberActorHandler ah){
		return spawn(DefaultFiberScheduler.getInstance(), ah);
	}
	
	/**
	 * 产生一个线程actor，并且使用指定的Handler来放入指定的执行器执行
	 * @param executor 执行器
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Executor executor, final IThreadActorHandler ah){
		if (executor == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(executor);
		ActorId aid = actor.getActorId();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Actor.runOnThread(actor, ah);
			}
		});
		return aid;
	}
	
	/**
	 * 产生一个纤程actor，并且使用指定的Handler来放入指定的调度器执行
	 * @param fibSche 调度器
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(FiberScheduler fibSche, final IFiberActorHandler ah){
		if (fibSche == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(fibSche);
		ActorId aid = actor.getActorId();
		new Fiber<Void>(fibSche) {
			private static final long serialVersionUID = 2841359941298581576L;
			@Override
			protected Void run() throws SuspendExecution, InterruptedException {
				Actor.runOnFiber(actor, ah);
				return null;
			}
		}.start();
		return aid;
	}
	
	/**
	 * 产生一个线程actor，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, IThreadActorHandler ah){
		return spawn(sire, ah, LinkType.NO_LINK);
	}
	
	/**
	 * 产生一个纤程actor，并且使用指定的Handler来放入默认调度器执行
	 * @param sire 父Actor
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, IFiberActorHandler ah){
		return spawn(sire, ah, LinkType.NO_LINK);
	}
	
	/**
	 * 产生一个线程actor，并且使用指定的Handler来放入指定的执行器执行
	 * @param sire 父Actor
	 * @param executor 执行器
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, Executor executor, IThreadActorHandler ah){
		return spawn(sire, executor, ah, LinkType.NO_LINK);
	}
	
	/**
	 * 产生一个纤程actor，并且使用指定的Handler来放入指定的调度器执行
	 * @param sire 父Actor
	 * @param fibSche 调度器
	 * @param ah 可运行单元
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, FiberScheduler fibSche, IFiberActorHandler ah){
		return spawn(sire, fibSche, ah, LinkType.NO_LINK);
	}

	/**
	 * 产生一个线程actor，指定和父Actor的链接关系，并且使用指定的Handler来放入线程池执行
	 * @param sire 父Actor
	 * @param ah 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, IThreadActorHandler ah, LinkType link){
		return spawn(sire, actorThreadPool, ah, link);
	}

	/**
	 * 产生一个纤程actor，指定和父Actor的链接关系，并且使用指定的Handler来放入默认的调度器执行
	 * @param sire 父Actor
	 * @param ah 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, IFiberActorHandler ah, LinkType link){
		return spawn(sire, DefaultFiberScheduler.getInstance(), ah, link);
	}

	/**
	 * 产生一个线程actor，指定和父Actor的链接关系，并且使用指定的Handler来放入指定的执行器执行
	 * @param sire 父Actor
	 * @param executor 执行器
	 * @param ah 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, Executor executor, final IThreadActorHandler ah, LinkType link){
		if (executor == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(executor);
		addLink(sire, actor, link);
		ActorId aid = actor.getActorId();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Actor.runOnThread(actor, ah);
			}
		});
		return aid;
	}

	/**
	 * 产生一个纤程actor，指定和父Actor的链接关系，并且使用指定的Handler来放入指定的调度器执行
	 * @param sire 父Actor
	 * @param fibSche 调度器
	 * @param ah 可运行单元
	 * @param link 链接关系类型
	 * @return 新Actor的Id
	 */
	@Suspendable
	public ActorId spawn(Actor sire, FiberScheduler fibSche, final IFiberActorHandler ah, LinkType link){
		if (fibSche == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(fibSche);
		addLink(sire, actor, link);
		ActorId aid = actor.getActorId();
		new Fiber<Void>(fibSche) {
			private static final long serialVersionUID = 2841359941298581576L;
			@Override
			protected Void run() throws SuspendExecution, InterruptedException {
				Actor.runOnFiber(actor, ah);
				return null;
			}
		}.start();
		return aid;
	}
	
	/**
	 * 产生一个ActorRef
	 * @param refAid
	 * @return
	 */
	public ActorRef ref(ActorId refAid){
		return new ActorRef(this, refAid);
	}
	
	/**
	 * 发送一个自己构建的消息
	 * @param toAid
	 * @param msg
	 */
	@Suspendable
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
	@Suspendable
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
	 */
	@Suspendable
	public void send(ActorId fromAid, ActorId toAid, String type){
		Message msg = Message.make();
		
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
	 */
	@Suspendable
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
	@Suspendable
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
	@Suspendable
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
	@Suspendable
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
	@Suspendable
	Actor getActor(ActorId aid){
		return actorMap.get(aid);
	}

	@Suspendable
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
		return new ActorId(axid, timestamp, id);
	}
	
	private static void addLink(Actor sire, Actor child, LinkType link){
		if (link == LinkType.LINKED){
			sire.addLink(child.getActorId());
			child.addLink(sire.getActorId());
		}else if (link == LinkType.MONITORED){
			child.addLink(sire.getActorId());
		}
	}

	@Suspendable
	private Actor makeActor(FiberScheduler fibSche){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, fibSche);
		actorMap.put(aid, actor);
		return actor;
	}

	@Suspendable
	private Actor makeActor(Executor executor){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, executor);
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
