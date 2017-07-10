/**
 * 
 */
package actorx;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import actorx.remote.LocalInfo;
import actorx.remote.NetworkManager;
import actorx.remote.RemoteInfo;
import actorx.util.Atom;
//import actorx.remote.LocalInfo;
//import actorx.remote.RemoteInfo;
//import actorx.remote.NetworkManager;
//import actorx.util.Atom;
import actorx.util.ConcurrentHashMap;
import actorx.util.ServiceUtils;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

/**
 * @author Xiong
 */
public class ActorSystem {
	// ActorSystemConfig
	private ActorSystemConfig ascfg;
	// ActorSystem unique id
	private long axid = 0;
	/** 启动时间戳 */
	private long timestamp;
	/** actor全局表 */
	private ConcurrentHashMap<ActorId, Actor> actorMap = new ConcurrentHashMap<ActorId, Actor>();
	/** Actor线程池 */
	private ExecutorService actorThreadPool;
	private boolean outerActorThreadPool = false;
	/** 网络线程池 */
	private ExecutorService networkThreadPool;
	private boolean outerNetworkThreadPool = false;
	/** ActorId分配基础 */
	private AtomicLong[] actorIdBases;
	private int currentIdBase = 0;
	// 自定义消息工厂map
	private ConcurrentHashMap<String, ICustomMessageFactory> customMsgMap = 
			new ConcurrentHashMap<String, ICustomMessageFactory>();
	private java.util.concurrent.ConcurrentHashMap<String, ICustomMessageFactory> cmsgMap = 
			new java.util.concurrent.ConcurrentHashMap<String, ICustomMessageFactory>();
	// 网络管理器
	private NetworkManager netMgr;
	
	
	public ActorSystem(){
		this(new ActorSystemConfig());
	}
	
	public ActorSystem(String axid){
		this(new ActorSystemConfig().setAxid(axid));
	}
	
	public ActorSystem(int threadNum){
		this(new ActorSystemConfig().setThreadNum(threadNum));
	}
	
	public ActorSystem(ActorSystemConfig ascfg){
		this.ascfg = ascfg;
		this.actorIdBases = makeActorIdBases(Runtime.getRuntime().availableProcessors());
		
		long axid = ascfg.getAxid();
		int threadNum = ascfg.getThreadNum();
		actorThreadPool = ascfg.getActorThreadPool();
		if (actorThreadPool != null){
			outerActorThreadPool = true;
		}
		networkThreadPool = ascfg.getNetworkThreadPool();
		if (networkThreadPool != null){
			outerNetworkThreadPool = true;
		}
		
		if (actorThreadPool == null){
			if (threadNum > 0){
				actorThreadPool = Executors.newFixedThreadPool(threadNum);
			}else{
				actorThreadPool = Executors.newCachedThreadPool();
			}
		}
		
		customMsgMap.putAll(ascfg.getCustomMessageList());
		cmsgMap.putAll(ascfg.getCustomMessageList());
//		for (Entry<String, ICustomMessageFactory> e : ascfg.getCustomMessageList().entrySet()){
//			customMsgMap.put(e.getKey(), e.getValue());
//		}

		if (axid != 0){
			this.axid = axid;
			this.netMgr = new NetworkManager(
				this, 
				ascfg.getInternalPoolSize(), 
				ascfg.getInternalPoolInitSize(), 
				ascfg.getInternalPoolMaxSize()
			);
			if (networkThreadPool == null){
				networkThreadPool = actorThreadPool;
			}
		}
		
		this.timestamp = System.currentTimeMillis();
	}

	@Suspendable
	public void startup(){
		if (netMgr != null){
			netMgr.start();
		}
	}

	@Suspendable
	public void shutdown(){
		if (netMgr != null){
			netMgr.stop();
		}
		
		if (!outerNetworkThreadPool && networkThreadPool != null){
			networkThreadPool.shutdown();
			try{
				networkThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}catch (InterruptedException e){
			}
		}
		
		if (!outerActorThreadPool && actorThreadPool != networkThreadPool){
			actorThreadPool.shutdown();
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
	
	public ActorSystemConfig getConfig() {
		return ascfg;
	}
	
	public ConcurrentHashMap<String, ICustomMessageFactory> getCustomMessageMap(){
		return customMsgMap;
	}
	
	public java.util.concurrent.ConcurrentHashMap<String, ICustomMessageFactory> getCMsgMap(){
		return cmsgMap;
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
	 * 获取网络管理器
	 * @return
	 */
	public NetworkManager getNetworkManager(){
		return netMgr;
	}
	
	/**
	 * 是否是本地actor
	 * @param aid
	 * @return
	 */
	public boolean isLocalActor(ActorId aid){
		return axid == aid.axid;
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
	
	@Suspendable
	public Actor spawn(String serviceName){
		ActorId aid = ServiceUtils.makeServiceId(this, serviceName);
		Actor actor = new Actor(this, aid);
		if (actorMap.putIfAbsent(aid, actor) != null){
			return null;
		}
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
	
	@Suspendable
	public Actor spawn(Actor sire, String serviceName){
		return spawn(sire, serviceName, LinkType.NO_LINK);
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

	@Suspendable
	public Actor spawn(Actor sire, String serviceName, LinkType link){
		Actor actor = spawn(serviceName);
		if (actor == null){
			return null;
		}
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
	
	@Suspendable
	public ActorId spawn(String serviceName, IThreadActorHandler ah){
		return spawn(actorThreadPool, serviceName, ah);
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

	@Suspendable
	public ActorId spawn(String serviceName, IFiberActorHandler ah){
		return spawn(DefaultFiberScheduler.getInstance(), serviceName, ah);
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
	
	@Suspendable
	public ActorId spawn(Executor executor, String serviceName, final IThreadActorHandler ah){
		if (executor == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(executor, serviceName);
		if (actor == null){
			return ActorId.NULL;
		}

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

	@Suspendable
	public ActorId spawn(FiberScheduler fibSche, String serviceName, final IFiberActorHandler ah){
		if (fibSche == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(fibSche, serviceName);
		if (actor == null){
			return ActorId.NULL;
		}
		
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

	@Suspendable
	public ActorId spawn(Actor sire, String serviceName, IThreadActorHandler ah){
		return spawn(sire, serviceName, ah, LinkType.NO_LINK);
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
	
	@Suspendable
	public ActorId spawn(Actor sire, String serviceName, IFiberActorHandler ah){
		return spawn(sire, serviceName, ah, LinkType.NO_LINK);
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

	@Suspendable
	public ActorId spawn(Actor sire, Executor executor, String serviceName, IThreadActorHandler ah){
		return spawn(sire, executor, serviceName, ah, LinkType.NO_LINK);
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
	
	@Suspendable
	public ActorId spawn(Actor sire, FiberScheduler fibSche, String serviceName, IFiberActorHandler ah){
		return spawn(sire, fibSche, serviceName, ah, LinkType.NO_LINK);
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
	
	@Suspendable
	public ActorId spawn(Actor sire, String serviceName, IThreadActorHandler ah, LinkType link){
		return spawn(sire, actorThreadPool, serviceName, ah, link);
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

	@Suspendable
	public ActorId spawn(Actor sire, String serviceName, IFiberActorHandler ah, LinkType link){
		return spawn(sire, DefaultFiberScheduler.getInstance(), serviceName, ah, link);
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

	@Suspendable
	public ActorId spawn(Actor sire, Executor executor, String serviceName, final IThreadActorHandler ah, LinkType link){
		if (executor == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(executor, serviceName);
		if (actor == null){
			return ActorId.NULL;
		}
		
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

	@Suspendable
	public ActorId spawn(Actor sire, FiberScheduler fibSche, String serviceName, final IFiberActorHandler ah, LinkType link){
		if (fibSche == null){
			throw new NullPointerException();
		}
		
		final Actor actor = makeActor(fibSche, serviceName);
		if (actor == null){
			return ActorId.NULL;
		}
		
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

	@Suspendable
	public void addRemote(String axid, RemoteInfo remoteInfo){
		if (remoteInfo == null){
			throw new NullPointerException();
		}
		
		netMgr.addRemote(Atom.to(axid), remoteInfo);
	}

	@Suspendable
	public boolean removeRemote(String axid){
		return netMgr.removeRemote(Atom.to(axid));
	}
	
	/**
	 * 监听本地
	 * @param localInfo
	 */
	@Suspendable
	public void listen(LocalInfo localInfo){
		netMgr.listen(localInfo);
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
	
	@Suspendable
	public <A, A1, A2, A3> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		if (arg5 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		if (arg5 == null){
			throw new NullPointerException();
		}
		if (arg6 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		if (arg5 == null){
			throw new NullPointerException();
		}
		if (arg6 == null){
			throw new NullPointerException();
		}
		if (arg7 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		msg.put(arg7);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		if (arg5 == null){
			throw new NullPointerException();
		}
		if (arg6 == null){
			throw new NullPointerException();
		}
		if (arg7 == null){
			throw new NullPointerException();
		}
		if (arg8 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		msg.put(arg7);
		msg.put(arg8);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8, A9> void send(ActorId fromAid, ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8, A9 arg9){
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		if (arg3 == null){
			throw new NullPointerException();
		}
		if (arg4 == null){
			throw new NullPointerException();
		}
		if (arg5 == null){
			throw new NullPointerException();
		}
		if (arg6 == null){
			throw new NullPointerException();
		}
		if (arg7 == null){
			throw new NullPointerException();
		}
		if (arg8 == null){
			throw new NullPointerException();
		}
		if (arg9 == null){
			throw new NullPointerException();
		}
		
		Message msg = Message.make();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		msg.put(arg7);
		msg.put(arg8);
		msg.put(arg9);
		
		msg.setSender(fromAid);
		msg.setType(type);
		if (!Actor.sendMessage(this, toAid, msg)){
			msg.release();
		}
	}
	
//	/**
//	 * 发送消息
//	 * @param fromAid
//	 * @param toAid
//	 * @param type
//	 * @param args 可以为null
//	 */
//	@Suspendable
//	public void send(ActorId fromAid, ActorId toAid, String type, Object... args){
//		Message msg = Message.make();
//		if (args != null){
//			for (Object arg : args){
//				if (arg == null){
//					msg.release();
//					throw new NullPointerException();
//				}
//				msg.put(arg);
//			}
//		}
//		
//		msg.setSender(fromAid);
//		msg.setType(type);
//		if (!Actor.sendMessage(this, toAid, msg)){
//			msg.release();
//		}
//	}
	
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
	private Actor makeActor(FiberScheduler fibSche, String serviceName){
		ActorId aid = ServiceUtils.makeServiceId(this, serviceName);
		Actor actor = new Actor(this, aid, fibSche);
		if (actorMap.putIfAbsent(aid, actor) != null){
			return null;
		}
		return actor;
	}

	@Suspendable
	private Actor makeActor(Executor executor){
		ActorId aid = generateActorId();
		Actor actor = new Actor(this, aid, executor);
		actorMap.put(aid, actor);
		return actor;
	}

	@Suspendable
	private Actor makeActor(Executor executor, String serviceName){
		ActorId aid = ServiceUtils.makeServiceId(this, serviceName);
		Actor actor = new Actor(this, aid, executor);
		if (actorMap.putIfAbsent(aid, actor) != null){
			return null;
		}
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
