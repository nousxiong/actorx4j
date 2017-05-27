/**
 * 
 */
package actorx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import actorx.detail.IMail;
import actorx.detail.Mailbox;
import actorx.detail.StrandSynchronizer;
import actorx.util.ContainerUtils;
import actorx.util.ExceptionUtils;
import cque.AbstractNode;
import cque.IPooledObject;
import cque.IRecycler;
import cque.IntrusiveSuspendedQueue;
import cque.NodeStack;
import cque.util.PoolGuard;

/**
 * @author Xiong
 */
public class Actor extends AbstractNode implements IRecycler {
	// AxSystem
	private ActorSystem axs;
	// ActorId
	private ActorId selfAid = null;
	// handler
	private FiberScheduler fibSche;
	private Executor executor;
	/**自己退出时需要发送EXIT消息的列表*/
	private Set<ActorId> linkList; // 自己主动链接别人
	private Set<ActorId> linkedList; // 自己被别人链接
	/**消息队列*/
//	private IntrusiveMpscQueue<Message> msgQue = new IntrusiveMpscQueue<Message>(new ReentrantLock());
	private IntrusiveSuspendedQueue<Message> msgQue = new IntrusiveSuspendedQueue<Message>(new StrandSynchronizer());
//	private SingleConsumerArrayObjectQueue<Message> msgQue = new SingleConsumerArrayObjectQueue<Message>(100000 * 10);
	/**池守卫缓存*/
	private PoolGuard poolGuardCache;
	/**邮箱*/
	private Mailbox mailbox;
	// 需要匹配的模式，临时数据
	private Pattern pattern;
	// 分类型发送Filters
	private Map<String, Set<ISendFilter>> sendFilters;
	// 接收Filters
	private Map<String, Set<IRecvFilter>> recvFilters;
	/**是否已经退出*/
	private boolean exited = false;
	/**链接锁*/
	private ReentrantLock linkLock = new ReentrantLock();
	
	/**
	 * 创建自行调度的Actor
	 * @param axs
	 * @param aid
	 */
	public Actor(ActorSystem axs, ActorId aid){
		this.axs = axs;
		this.selfAid = aid;
	}
	
	/**
	 * 创建基于线程的actor
	 * @param axs
	 * @param aid
	 * @param threadHandler
	 */
	public Actor(ActorSystem axs, ActorId aid, Executor executor){
		this.axs = axs;
		this.selfAid = aid;
		this.executor = executor;
	}
	
	/**
	 * 创建基于纤程的Actor
	 * @param axs
	 * @param aid
	 */
	public Actor(ActorSystem axs, ActorId aid, FiberScheduler fibSche){
		this.axs = axs;
		this.selfAid = aid;
		this.fibSche = fibSche;
	}
	
	/**
	 * 返回AxSystem
	 * @return
	 */
	public ActorSystem getActorSystem(){
		return axs;
	}
	
	/**
	 * 返回纤程调度器，如果使用纤程创建的Actor，则返回不为空
	 * @return
	 */
	public FiberScheduler getFiberScheduler() {
		return fibSche;
	}

	/**
	 * 返回线程调度器，如果使用线程创建的Actor，则返回不为空
	 * @return
	 */
	public Executor getThreadExecutor() {
		return executor;
	}

	/**
	 * 返回一个自身的引用
	 * @return
	 */
	public ActorRef ref(){
		return axs.ref(selfAid);
	}
	
	/**
	 * 创建消息
	 * @return
	 */
	public PoolGuard makeMessage(){
		return makeGuard(makeNewMessage());
	}
	
	/**
	 * 链接指定的actor；如果链接成功，则返回{@link AtomCode#LINK}；如果对方已经退出，则返回{@link AtomCode#EXIT}
	 * @param aid
	 */
	@Suspendable
	public void link(ActorId aid){
		link(aid, false);
	}
	
	/**
	 * 监视指定的actor；如果链接成功，则返回{@link AtomCode#MONITOR}；如果对方已经退出，则返回{@link AtomCode#EXIT}
	 * @param aid
	 */
	@Suspendable
	public void monitor(ActorId aid){
		link(aid, true);
	}
	
	/**
	 * 发送一个空消息
	 * @param toAid
	 */
	@Suspendable
	public void send(ActorId toAid){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Message msg = makeNewMessage();
		sendMessage(toAid, selfAid, AtomCode.NULLTYPE, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid
	 * @param type
	 */
	@Suspendable
	public void send(ActorId toAid, String type){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Message msg = makeNewMessage();
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A> void send(ActorId toAid, String type, A arg){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (arg == null){
			throw new NullPointerException();
		}
		
		Message msg = makeNewMessage();
		msg.put(arg);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1> void send(ActorId toAid, String type, A arg, A1 arg1){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (arg == null){
			throw new NullPointerException();
		}
		if (arg1 == null){
			throw new NullPointerException();
		}
		if (arg2 == null){
			throw new NullPointerException();
		}
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		msg.put(arg7);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
		msg.put(arg);
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		msg.put(arg4);
		msg.put(arg5);
		msg.put(arg6);
		msg.put(arg7);
		msg.put(arg8);
		sendMessage(toAid, selfAid, type, msg);
	}
	
	@Suspendable
	public <A, A1, A2, A3, A4, A5, A6, A7, A8, A9> void send(ActorId toAid, String type, A arg, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6, A7 arg7, A8 arg8, A9 arg9){
		if (isExited()){
			throw new IllegalStateException();
		}
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
		
		Message msg = makeNewMessage();
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
		sendMessage(toAid, selfAid, type, msg);
	}
	
//	/**
//	 * 发送消息
//	 * @param toAid
//	 * @param type
//	 * @param args 可以为null
//	 */
//	@Suspendable
//	public void send(ActorId toAid, String type, Object... args){
//		if (isExited()){
//			throw new IllegalStateException();
//		}
//		
//		Message msg = makeNewMessage();
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
//		sendMessage(toAid, selfAid, type, msg);
//	}
	
	/**
	 * 发送消息
	 * @param toAid 接收者
	 * @param msg
	 */
	@Suspendable
	public void send(ActorId toAid, Message src){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (src == null){
			throw new NullPointerException();
		}

		Message msg = makeMessage(src);
		sendMessage(toAid, selfAid, msg.getType(), msg);
	}
	
	/**
	 * 转发一个消息；被转发的消息的sender会保持原始的sender
	 * @param toAid
	 * @param msg
	 */
	@Suspendable
	public void relay(ActorId toAid, Message src){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (src == null){
			throw new NullPointerException();
		}
		
		Message msg = makeMessage(src);
		sendMessage(toAid, msg.getSender(), msg.getType(), msg);
	}

	/**
	 * 发送自定义消息（Custom Send)
	 * @param toAid
	 * @param msg
	 */
	@Suspendable
	public <C extends Message> void csend(ActorId toAid, C msg){
		if (isExited()){
			throw new IllegalStateException();
		}

		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(toAid, selfAid, msg.getType(), msg);
	}

	/**
	 * 发送自定义消息（Custom Send)
	 * @param toAid
	 * @param type
	 * @param msg
	 */
	@Suspendable
	public <C extends Message> void csend(ActorId toAid, String type, C msg){
		if (isExited()){
			throw new IllegalStateException();
		}

		if (msg == null){
			throw new NullPointerException();
		}
		
		sendMessage(toAid, selfAid, type, msg);
	}

	@Suspendable
	public <C extends Message> void crelay(ActorId toAid, C msg){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		
		sendMessage(toAid, msg.getSender(), msg.getType(), msg);
	}
	
	/**
	 * 添加分类型的发送Filter
	 * @param sendFilter
	 */
	public void addSendFilter(String type, ISendFilter sendFilter){
		Map<String, Set<ISendFilter>> sendFilters = getSendFilters();
		Set<ISendFilter> sendFilterSet = sendFilters.get(type);
		if (sendFilterSet == null){
			sendFilterSet = new HashSet<ISendFilter>();
			sendFilters.put(type, sendFilterSet);
		}
		sendFilterSet.add(sendFilter);
	}
	
	/**
	 * 移除发送Filter
	 * @param sendFilter
	 */
	public void removeSendFilter(String type, ISendFilter sendFilter){
		if (!ContainerUtils.isEmpty(sendFilters)){
			Set<ISendFilter> sendFilterSet = sendFilters.get(type);
			if (!ContainerUtils.isEmpty(sendFilterSet)){
				sendFilterSet.remove(sendFilter);
			}
		}
	}
	
	/**
	 * 添加按类型接收Filter
	 * @param type
	 * @param recvFilter
	 */
	public void addRecvFilter(String type, IRecvFilter recvFilter){
		Map<String, Set<IRecvFilter>> recvFilters = getRecvFilters();
		Set<IRecvFilter> recvFilterSet = recvFilters.get(type);
		if (recvFilterSet == null){
			recvFilterSet = new HashSet<IRecvFilter>();
			recvFilters.put(type, recvFilterSet);
		}
		recvFilterSet.add(recvFilter);
	}
	
	/**
	 * 移除按类型接收Filter
	 * @param type
	 * @param recvFilter
	 */
	public void removeRecvFilter(String type, IRecvFilter recvFilter){
		if (!ContainerUtils.isEmpty(recvFilters)){
			Set<IRecvFilter> recvFilterSet = recvFilters.get(type);
			if (!ContainerUtils.isEmpty(recvFilterSet)){
				recvFilterSet.remove(recvFilter);
			}
		}
	}
	
//	/**
//	 * 创建BatchSender
//	 * @return
//	 */
//	public BatchSender makeBatchSender(){
//		return new BatchSender(this);
//	}
	
	/**
	 * 接收指定匹配的消息
	 * @param type
	 * @return
	 */
	@Suspendable
	public Message recv(String type){
		return recv(Message.NULL, type);
	}

	@Suspendable
	public Message recv(Message cmsg, String type){
		Pattern pattern = getPattern();
		pattern.match(type);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param type1
	 * @param type2
	 * @return
	 */
	@Suspendable
	public Message recv(String type1, String type2){
		return recv(Message.NULL, type1, type2);
	}

	@Suspendable
	public Message recv(Message cmsg, String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(type1);
		pattern.match(type2);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param types
	 * @return
	 */
	@Suspendable
	public Message recv(String... types){
		return recv(Message.NULL, types);
	}

	@Suspendable
	public Message recv(Message cmsg, String... types){
		Pattern pattern = getPattern();
		for (String type : types){
			pattern.match(type);
		}
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	@Suspendable
	public Message recv(ActorId sender){
		return recv(Message.NULL, sender);
	}

	@Suspendable
	public Message recv(Message cmsg, ActorId sender){
		Pattern pattern = getPattern();
		pattern.match(sender);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	@Suspendable
	public Message recv(ActorId sender, String type){
		return recv(Message.NULL, sender, type);
	}

	@Suspendable
	public Message recv(Message cmsg, ActorId sender, String type){
		Pattern pattern = getPattern();
		pattern.match(sender, type);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	@Suspendable
	public Message recv(ActorId sender, String type1, String type2){
		return recv(Message.NULL, sender, type1, type2);
	}

	@Suspendable
	public Message recv(Message cmsg, ActorId sender, String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(sender, type1, type2);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	@Suspendable
	public Message recv(ActorId sender, String... types){
		return recv(Message.NULL, sender, types);
	}

	@Suspendable
	public Message recv(Message cmsg, ActorId sender, String... types){
		Pattern pattern = getPattern();
		pattern.match(sender, types);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收消息
	 * @return
	 */
	@Suspendable
	public Message recv(){
		return recv(Message.NULL);
	}

	@Suspendable
	public Message recv(Message cmsg){
		Pattern pattern = getPattern();
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @return
	 */
	@Suspendable
	public Message recv(long timeout){
		return recv(Message.NULL, timeout);
	}

	@Suspendable
	public Message recv(Message cmsg, long timeout){
		Pattern pattern = getPattern();
		pattern.after(timeout);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	@Suspendable
	public Message recv(long timeout, TimeUnit timeUnit){
		return recv(Message.NULL, timeout, timeUnit);
	}

	@Suspendable
	public Message recv(Message cmsg, long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.after(timeout, timeUnit);
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}
	
	/**
	 * 接收指定模式的消息
	 * @param patt
	 * @return
	 */
	@Suspendable
	public Message recv(Pattern pattern){
		return recv(Message.NULL, pattern);
	}
	
	@Suspendable
	public Message recv(Message cmsg, Pattern pattern){
		Message msg = recvMessage(pattern);
		return Message.move(msg, cmsg);
	}

	///------------------------------------------------------------------------
	/// 接收退出消息
	///------------------------------------------------------------------------
	/**
	 * 接收{@link AtomCode#EXIT}消息
	 * @return
	 */
	@Suspendable
	public ActorExit recvExit(){
		return recvExit(Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}

	/**
	 * 
	 * @param sender
	 * @return
	 */
	@Suspendable
	public ActorExit recvExit(ActorId sender){
		return recvExit(sender, Pattern.DEFAULT_TIMEOUT);
	}
	
	/**
	 * 接收{@link AtomCode#EXIT}消息
	 * @param timeout
	 * @return
	 */
	@Suspendable
	public ActorExit recvExit(long timeout){
		return recvExit(timeout, Pattern.DEFAULT_TIMEUNIT);
	}

	@Suspendable
	public ActorExit recvExit(ActorId sender, long timeout){
		return recvExit(sender, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收{@link AtomCode#EXIT}消息
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	@Suspendable
	public ActorExit recvExit(long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.match(AtomCode.EXIT);
		try (PoolGuard guard = precv(pattern)){
			Message msg = guard.get();
			if (msg == null){
				return null;
			}
			
			ActorExit axExit = msg.get(ActorExit.class);
			axExit.setSender(msg.getSender());
			return axExit;
		}catch (Throwable e){
			return null;
		}
	}

	@Suspendable
	public ActorExit recvExit(ActorId sender, long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.match(sender, AtomCode.EXIT);
		try (PoolGuard guard = precv(pattern)){
			Message msg = guard.get();
			if (msg == null){
				return null;
			}
			
			ActorExit axExit = msg.get(ActorExit.class);
			axExit.setSender(msg.getSender());
			return axExit;
		}catch (Throwable e){
			return null;
		}
	}
	
	/**
	 * 运行Actor时，首先调用
	 */
	public void init(){
	}

	/**
	 * 正常退出
	 */
	@Suspendable
	public void quit(){
		quit(new ActorExit(ExitType.NORMAL, "no error"));
	}

	/**
	 * 指定退出类型和可能的错误信息；可以调用多次，从第二次之后，就自动忽略
	 * @param et
	 * @param errmsg
	 */
	@Suspendable
	public void quit(ActorExit axExit){
		if (!axs.removeActor(selfAid)){
			return;
		}

		linkLock.lock();
//		synchronized (this){
		try{
			// 发送退出消息给所有链接的Actor
			if (!ContainerUtils.isEmpty(linkList)){
				for (ActorId aid : linkList){
					send(aid, AtomCode.EXIT, axExit);
				}
			}
			
			if (!ContainerUtils.isEmpty(linkedList)){
				for (ActorId aid : linkedList){
					send(aid, AtomCode.EXIT, axExit);
				}
			}
			
			exited = true;
		}finally{
			linkLock.unlock();
		}
		
		// 释放剩余未处理的消息
		if (mailbox != null){
			mailbox.clear();
		}
		
		while (true){
			Message msg = msgQue.poll();
			if (msg == null){
				break;
			}
			msg.release();
		}
	}
	
	/**
	 * 取得自己的ActorId
	 * @return
	 */
	public ActorId getActorId(){
		return selfAid;
	}
	
	/**
	 * 是否已经退出
	 * @return
	 */
	public boolean isExited(){
		return exited;
	}
	
	/**
	 * 发送消息
	 * @param sender
	 * @param recvAid
	 * @param msg
	 */
	@Suspendable
	public static boolean sendMessage(ActorSystem axs, ActorId recvAid, Message msg){
		Actor a = axs.getActor(recvAid);
		if (a == null){
			return false;
		}
		
		a.addMessage(msg);
		return true;
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	@Suspendable
	public PoolGuard precv(Pattern pattern){
		return makeGuard(recvMessage(pattern));
	}
	
//	/**
//	 * 批量发送
//	 * @param toAid 接收者
//	 * @param bmsg
//	 */
//	void batchSend(ActorId toAid, Message bmsg){
//		if (isExited()){
//			throw new IllegalStateException();
//		}
//		if (bmsg == null){
//			throw new NullPointerException();
//		}
//
////		Message msg = makeMessage(src);
////		sendMessage(toAid, selfAid, bmsg.getType(), bmsg);
//
//		if (bmsg != null && !sendMessage(axs, toAid, bmsg)){
//			bmsg.release();
//		}
//	}
	
	void addLink(ActorId target){
		Set<ActorId> linkList = getLinkList();
		linkList.add(target);
	}
	
	void addLinked(ActorId target){
		Set<ActorId> linkedList = getLinkedList();
		linkedList.add(target);
	}

	static void runOnFiber(Actor actor, final IFiberActorHandler fiberHandler) throws SuspendExecution, InterruptedException{
		ActorExit axExit = new ActorExit(ExitType.NORMAL, "no error");
		try{
			actor.init();
			fiberHandler.run(actor);
		}catch (Throwable e){
			axExit.setExitType(ExitType.EXCEPT);
			String errmsg = ExceptionUtils.printStackTrace(e);
			axExit.setErrmsg(errmsg);
		}finally{
			actor.quit(axExit);
		}
	}
	
	static void runOnThread(Actor actor, final IThreadActorHandler threadHandler){
		ActorExit axExit = new ActorExit(ExitType.NORMAL, "no error");
		try{
			actor.init();
			threadHandler.run(actor);
		}catch (Throwable e){
			axExit.setExitType(ExitType.EXCEPT);
			String errmsg = ExceptionUtils.printStackTrace(e);
			axExit.setErrmsg(errmsg);
		}finally{
			actor.quit(axExit);
		}
	}

	@Suspendable
	public static ActorExit linkFromRemote(ActorSystem axSys, ActorId remoteAid, ActorId localAid){
		Actor ax = axSys.getActor(localAid);
		if (ax == null){
			return new ActorExit(ExitType.ALREADY, "already exited");
		}
		
//		String type = isMonitor ? AtomCode.MONITOR : AtomCode.LINK;
		ReentrantLock linkLock = ax.linkLock;
		linkLock.lock();
		try{
			// 获取锁后查一次此actor是否已经退出
			if (ax.isExited()){
				return new ActorExit(ExitType.ALREADY, "already exited");
			}
			
			ax.addLinked(remoteAid);
			return null;
		}finally{
			linkLock.unlock();
		}
	}

	@Suspendable
	private void link(ActorId aid, boolean isMonitor){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Actor ax = axs.getActor(aid);
		if (ax == null){
			axs.send(aid, selfAid, AtomCode.EXIT, new ActorExit(ExitType.ALREADY, "already exited"));
			return;
		}
		
		String type = isMonitor ? AtomCode.MONITOR : AtomCode.LINK;
		ReentrantLock linkLock = ax.linkLock;
		linkLock.lock();
//		synchronized (ax){
		try{
			// 获取锁后查一次此actor是否已经退出
			if (ax.isExited()){
				axs.send(aid, selfAid, AtomCode.EXIT, new ActorExit(ExitType.ALREADY, "already exited"));
				return;
			}
			
			if (!isMonitor){
				// 将对方加入自己的link列表
				addLink(aid);
			}
			// 将自己加入对方linked列表
			ax.addLinked(selfAid);
			// 给自己发送监视成功的消息
			axs.send(aid, selfAid, type);
		}finally{
			linkLock.unlock();
		}
	}

	@Suspendable
	private void sendMessage(ActorId recvAid, ActorId sendAid, String type, Message msg){
		msg.setSender(sendAid);
		msg.setType(type);
		
		// 再尝试过滤按类型
		if (!ContainerUtils.isEmpty(sendFilters)){
			Set<ISendFilter> sendFilterSet = sendFilters.get(type);
			if (!ContainerUtils.isEmpty(sendFilterSet)){
				Message prevMsg = msg;
				for (ISendFilter sendFilter : sendFilterSet){
					Message filteredMsg = sendFilter.filterSend(recvAid, type, prevMsg, msg);
					if (filteredMsg != prevMsg && prevMsg != null && prevMsg != msg){
						prevMsg.release();
					}
					prevMsg = filteredMsg;
					if (prevMsg != null){
						prevMsg.resetRead();
					}
				}

				if (msg != prevMsg){
					msg.release();
				}
				msg = prevMsg;
			}
		}
		
		if (msg != null && !sendMessage(axs, recvAid, msg)){
			msg.release();
		}
	}

	@Suspendable
	private void addMessage(Message msg){
		msgQue.put(msg);
//		msgQue.offer(msg);
	}
	
	@Suspendable
	private Message recvMessage(Pattern pattern){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		List<String> matchedTypes = pattern.getMatchedTypes();
		List<Object> matchedActors = pattern.getMatchedActors();
		long timeout = pattern.getTimeout();
		TimeUnit timeUnit = pattern.getTimeUnit();
		
		Message msg = null;
		if (mailbox != null && !mailbox.isEmpty()){
			IMail mail = mailbox.fetch(matchedTypes, matchedActors, recvFilters);
			if (mail != null){
				msg = (Message) mail;
//				return makeGuard(msg);
				return msg;
			}
		}
		
		long currTimeout = timeUnit.toNanos(timeout);
		while (true){
			long bt = 0;
			long eclipse = 0;
			if (currTimeout > 0 && currTimeout < Long.MAX_VALUE){
				bt = System.nanoTime();
			}
			
			msg = msgQue.poll(currTimeout, TimeUnit.NANOSECONDS);
//			msg = msgQue.poll();
			if (msg == null){
				break;
			}
			
//			Message bmsg = msg;
//			boolean batchMsg = StringUtils.equals(AtomCode.BATCHMSG, msg.getType());
//			final int argsSize = batchMsg ? msg.argsSize() : 1;
//			for (int i=0; i<argsSize; ++i){
//				if (batchMsg){
//					msg = bmsg.getMsg(tmsg);
//					if (tmsg == msg){
//						tmsg = MessagePool.borrowObject();
//					}
//				}
			if (AtomCode.equals(AtomCode.EXIT, msg.getType())){
				// 从link和linked列表移除
				ActorId aid = msg.getSender();
				if (!ContainerUtils.isEmpty(linkList)){
					linkList.remove(aid);
				}
				
				if (!ContainerUtils.isEmpty(linkedList)){
					linkedList.remove(aid);
				}
			}
			
			msg = Mailbox.filter(msg, recvFilters);
			if (msg == null){
				continue;
			}
			
			boolean typesEmpty = ContainerUtils.isEmpty(matchedTypes);
			boolean actorsEmpty = ContainerUtils.isEmpty(matchedActors);
			if (typesEmpty && actorsEmpty){
//					break;
//					if (batchMsg){
//						endBatch(bmsg, argsSize - i - 1);
//					}
				return msg;
			}
			
			boolean found = Mailbox.match(msg, matchedTypes, matchedActors);
			if (found){
//					break;
//					if (batchMsg){
//						endBatch(bmsg, argsSize - i - 1);
//					}
				return msg;
			}else{
				Mailbox mailbox = getMailbox();
				mailbox.add(msg);
			}
//			}
			
			if (bt > 0){
				eclipse = System.nanoTime() - bt;
				if (eclipse < currTimeout){
					currTimeout -= eclipse;
				}else{
					currTimeout = 0;
				}
			}
			
			if (currTimeout == 0){
//				break;
				return msg;
			}
		}
		return msg;
	}
	
//	private void endBatch(Message bmsg, int lastSize){
//		if (lastSize > 0){
//			Mailbox mailbox = getMailbox();
//			for (int i=0; i<lastSize; ++i){
//				Message msg = bmsg.getMsg(tmsg);
//				if (tmsg == msg){
//					tmsg = MessagePool.borrowObject();
//				}
//				mailbox.add(msg);
//			}
//		}
//		bmsg.release();
//	}
	
	private PoolGuard makeGuard(Message msg){
		PoolGuard poolGuard = poolGuardCache;
		poolGuardCache = NodeStack.pop(poolGuardCache);
		if (poolGuard == null){
			poolGuard = new PoolGuard();
		}
		poolGuard.onBorrowed(this);
		poolGuard.protect(msg);
		return poolGuard;
	}
	
	private Message makeNewMessage(){
		return Message.make();
	}
	
	private Message makeMessage(Message src){
		if (src != null){
			return Message.make(src);
		}else{
			return Message.make();
		}
	}
	
	private Set<ActorId> getLinkList(){
		if (linkList == null){
			linkList = new HashSet<ActorId>();
		}
		return linkList;
	}
	
	private Set<ActorId> getLinkedList(){
		if (linkedList == null){
			linkedList = new HashSet<ActorId>();
		}
		return linkedList;
	}
	
	private Mailbox getMailbox(){
		if (mailbox == null){
			mailbox = new Mailbox();
		}
		return mailbox;
	}
	
	private Pattern getPattern(){
		if (pattern == null){
			pattern = new Pattern();
		}else{
			pattern.clear();
		}
		return pattern;
	}
	
	private Map<String, Set<ISendFilter>> getSendFilters(){
		if (sendFilters == null){
			sendFilters = new HashMap<String, Set<ISendFilter>>();
		}
		return sendFilters;
	}
	
	private Map<String, Set<IRecvFilter>> getRecvFilters(){
		if (recvFilters == null){
			recvFilters = new HashMap<String, Set<IRecvFilter>>();
		}
		return recvFilters;
	}

	@Override
	public void returnObject(IPooledObject po) {
		if (po == null){
			return;
		}

		po.onReturn();
		if (po instanceof PoolGuard){
			PoolGuard poolGuard = (PoolGuard) po;
			poolGuardCache = NodeStack.push(poolGuardCache, poolGuard);
		}
	}
}
