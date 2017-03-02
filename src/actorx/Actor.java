/**
 * 
 */
package actorx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import actorx.detail.IMail;
import actorx.detail.Mailbox;
import actorx.detail.MessageGuardFactory;
import actorx.util.ContainerUtils;
import actorx.util.ExceptionUtils;
import cque.IntrusiveMpscQueue;
import cque.MpscNodePool;
import cque.SimpleNodePool;

/**
 * @author Xiong
 */
public class Actor implements Runnable {
	// AxSystem
	private ActorSystem axs;
	// ActorId
	private ActorId selfAid = null;
	// handler
	private IActorHandler handler;
	/**自己退出时需要发送EXIT消息的列表*/
	private Set<ActorId> linkList;
	/**消息队列*/
	private IntrusiveMpscQueue<Message> msgQue = new IntrusiveMpscQueue<Message>();
	/**邮箱*/
	private Mailbox mailbox;
	// 消息守护者池
	private SimpleNodePool<MessageGuard> msgGuardPool = 
		new SimpleNodePool<MessageGuard>(new MessageGuardFactory());
	// 本地消息池
	private MpscNodePool<Message> msgPool;
	// 需要匹配的模式，临时数据
	private Pattern pattern;
	// 分类型发送Filters
	private Map<String, Set<ISendFilter>> sendFilters;
	// 接收Filters
	private Map<String, Set<IRecvFilter>> recvFilters;
	/**是否已经退出*/
	private boolean exited = false;
	
	
	/**
	 * 创建Actor
	 * @param axs
	 * @param aid
	 */
	public Actor(ActorSystem axs, ActorId aid, IActorHandler handler){
		this.axs = axs;
		this.selfAid = aid;
		this.handler = handler;
	}
	
	/**
	 * 返回AxSystem
	 * @return
	 */
	public ActorSystem getActorSystem(){
		return axs;
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
	public MessageGuard makeMessage(){
		return returnMessage(makeNewMessage());
	}
	
	/**
	 * 链接指定的actor；如果链接成功，则返回{@link MsgType#LINK}；如果对方已经退出，则返回{@link MsgType#EXIT}
	 * @param aid
	 */
	public void link(ActorId aid){
		link(aid, false);
	}
	
	/**
	 * 监视指定的actor；如果链接成功，则返回{@link MsgType#MONITOR}；如果对方已经退出，则返回{@link MsgType#EXIT}
	 * @param aid
	 */
	public void monitor(ActorId aid){
		link(aid, true);
	}
	
	/**
	 * 发送一个空消息
	 * @param toAid
	 */
	public void send(ActorId toAid){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Message msg = makeNewMessage();
		sendMessage(toAid, MsgType.NULLTYPE, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid
	 * @param type
	 * @param arg
	 */
	public <A> void send(ActorId toAid, String type, A arg){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (arg == null){
			throw new NullPointerException();
		}
		
		Message msg = makeNewMessage();
		msg.put(arg);
		sendMessage(toAid, type, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid
	 * @param type
	 * @param arg
	 * @param arg1
	 */
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
		sendMessage(toAid, type, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid
	 * @param type
	 * @param arg
	 * @param arg1
	 * @param arg2
	 */
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
		sendMessage(toAid, type, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid
	 * @param type
	 * @param args 可以为null
	 */
	public void send(ActorId toAid, String type, Object... args){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Message msg = makeNewMessage();
		if (args != null){
			for (Object arg : args){
				if (arg == null){
					msg.release();
					throw new NullPointerException();
				}
				msg.put(arg);
			}
		}
		
		sendMessage(toAid, type, msg);
	}
	
	/**
	 * 发送消息
	 * @param toAid 接收者
	 * @param msg
	 */
	public void send(ActorId toAid, Message src){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (src == null){
			throw new NullPointerException();
		}

		Message msg = makeMessage(src);
		sendMessage(toAid, msg.getType(), msg);
	}
	
	/**
	 * 转发一个消息；被转发的消息的sender会保持原始的sender
	 * @param toAid
	 * @param msg
	 */
	public void relay(ActorId toAid, Message src){
		if (isExited()){
			throw new IllegalStateException();
		}
		if (src == null){
			throw new NullPointerException();
		}
		
		Message msg = makeMessage(src);
		sendMessage(toAid, msg.getType(), msg);
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
	
	/**
	 * 接收指定匹配的消息
	 * @param type
	 * @return
	 */
	public MessageGuard recv(String type){
		Pattern pattern = getPattern();
		pattern.match(type);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param type1
	 * @param type2
	 * @return
	 */
	public MessageGuard recv(String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(type1);
		pattern.match(type2);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param types
	 * @return
	 */
	public MessageGuard recv(String... types){
		Pattern pattern = getPattern();
		for (String type : types){
			pattern.match(type);
		}
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public MessageGuard recv(ActorId sender){
		Pattern pattern = getPattern();
		pattern.match(sender);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public MessageGuard recv(ActorId sender, String type){
		Pattern pattern = getPattern();
		pattern.match(sender, type);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public MessageGuard recv(ActorId sender, String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(sender, type1, type2);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public MessageGuard recv(ActorId sender, String... types){
		Pattern pattern = getPattern();
		pattern.match(sender, types);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收消息
	 * @return
	 */
	public MessageGuard recv(){
		Pattern pattern = getPattern();
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @return
	 */
	public MessageGuard recv(long timeout){
		Pattern pattern = getPattern();
		pattern.after(timeout);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public MessageGuard recv(long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.after(timeout, timeUnit);
		return recvMessage(pattern);
	}
	
	/**
	 * 接收指定模式的消息
	 * @param patt
	 * @return
	 */
	public MessageGuard recv(Pattern pattern){
		return recvMessage(pattern);
	}

	///------------------------------------------------------------------------
	/// 使用Packet接收
	///------------------------------------------------------------------------
	/**
	 * 接收只读数据
	 * @param type
	 * @return
	 */
	public Packet recvPacket(String type){
		return recvPacket(Packet.NULL, type);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type
	 * @return
	 */
	public Packet recvPacket(Packet pkt, String type){
		Pattern pattern = getPattern();
		pattern.match(type);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收只读数据
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recvPacket(String type, String type1){
		return recvPacket(Packet.NULL, type, type1);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recvPacket(Packet pkt, String type, String type1){
		Pattern pattern = getPattern();
		pattern.match(type);
		pattern.match(type1);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收只读数据
	 * @param types
	 * @return
	 */
	public Packet recvPacket(String... types){
		return recvPacket(Packet.NULL, types);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param types
	 * @return
	 */
	public Packet recvPacket(Packet pkt, String... types){
		Pattern pattern = getPattern();
		for (String type : types){
			pattern.match(type);
		}
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收只读数据
	 * @param sender
	 * @return
	 */
	public Packet recvPacket(ActorId sender){
		return recvPacket(Packet.NULL, sender);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param pkt
	 * @param sender
	 * @return
	 */
	public Packet recvPacket(Packet pkt, ActorId sender){
		Pattern pattern = getPattern();
		pattern.match(sender);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @param type
	 * @return
	 */
	public Packet recvPacket(ActorId sender, String type){
		return recvPacket(Packet.NULL, sender, type);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param pkt
	 * @param sender
	 * @param type
	 * @return
	 */
	public Packet recvPacket(Packet pkt, ActorId sender, String type){
		Pattern pattern = getPattern();
		pattern.match(sender, type);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recvPacket(ActorId sender, String type, String type1){
		return recvPacket(Packet.NULL, sender, type, type1);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param pkt
	 * @param sender
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recvPacket(Packet pkt, ActorId sender, String type, String type1){
		Pattern pattern = getPattern();
		pattern.match(sender, type, type1);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @param types
	 * @return
	 */
	public Packet recvPacket(ActorId sender, String... types){
		return recvPacket(Packet.NULL, sender, types);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param pkt
	 * @param sender
	 * @param types
	 * @return
	 */
	public Packet recvPacket(Packet pkt, ActorId sender, String... types){
		Pattern pattern = getPattern();
		pattern.match(sender, types);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收只读数据
	 * @return
	 */
	public Packet recvPacket(){
		return recvPacket(Packet.NULL);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @return
	 */
	public Packet recvPacket(Packet pkt){
		Pattern pattern = getPattern();
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收指定超时时间的只读数据
	 * @param timeout
	 * @return
	 */
	public Packet recvPacket(long timeout){
		return recvPacket(Packet.NULL, timeout);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @return
	 */
	public Packet recvPacket(Packet pkt, long timeout){
		Pattern pattern = getPattern();
		pattern.after(timeout);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收指定超时时间的只读数据
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public Packet recvPacket(long timeout, TimeUnit timeUnit){
		return recvPacket(Packet.NULL, timeout, timeUnit);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public Packet recvPacket(Packet pkt, long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.after(timeout, timeUnit);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 接收只读数据
	 * @param pattern
	 * @return
	 */
	public Packet recvPacket(Pattern pattern){
		return recvPacket(Packet.NULL, pattern);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param pattern
	 * @return
	 */
	public Packet recvPacket(Packet pkt, Pattern pattern){
		return recvMessage(pkt, pattern);
	}

	///------------------------------------------------------------------------
	/// 接收退出消息
	///------------------------------------------------------------------------
	/**
	 * 接收{@link MsgType#EXIT}消息
	 * @return
	 */
	public ActorExit recvExit(){
		return recvExit(Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收{@link MsgType#EXIT}消息
	 * @param timeout
	 * @return
	 */
	public ActorExit recvExit(long timeout){
		return recvExit(timeout, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收{@link MsgType#EXIT}消息
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public ActorExit recvExit(long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.match(MsgType.EXIT);
		try (MessageGuard guard = recv(pattern)){
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
	 * 运行Actor线程时，首先调用
	 */
	public void init(){
		if (handler != null){
			msgPool = MessagePool.getLocalPool(MessagePool.fetchInitList());
		}
	}

	/**
	 * 正常退出
	 */
	public void quit(){
		quit(new ActorExit(ExitType.NORMAL, "no error"));
	}

	/**
	 * 指定退出类型和可能的错误信息；可以调用多次，从第二次之后，就自动忽略
	 * @param et
	 * @param errmsg
	 */
	public void quit(ActorExit axExit){
		if (!axs.removeActor(selfAid)){
			return;
		}

		synchronized (this){
			// 发送退出消息给所有链接的Actor
			if (!ContainerUtils.isEmpty(linkList)){
				for (ActorId aid : linkList){
					send(aid, MsgType.EXIT, axExit);
				}
			}
			
			exited = true;
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
	void addLink(ActorId target){
		Set<ActorId> linkList = getLinkList();
		linkList.add(target);
	}

	@Override
	public void run() {
		ActorExit axExit = new ActorExit(ExitType.NORMAL, "no error");
		try{
			init();
			handler.run(this);
		}catch (Throwable e){
			axExit.setExitType(ExitType.EXCEPT);
			String errmsg = ExceptionUtils.printStackTrace(e);
			axExit.setErrmsg(errmsg);
		}finally{
			quit(axExit);
		}
	}
	
	private void link(ActorId aid, boolean isMonitor){
		if (isExited()){
			throw new IllegalStateException();
		}
		
		Actor ax = axs.getActor(aid);
		if (ax == null){
			axs.send(aid, selfAid, MsgType.EXIT, new ActorExit(ExitType.ALREADY, "already exited"));
			return;
		}
		
		String type = isMonitor ? MsgType.MONITOR : MsgType.LINK;
		synchronized (ax){
			// 获取锁后查一次此actor是否已经退出
			if (ax.isExited()){
				axs.send(aid, selfAid, MsgType.EXIT, new ActorExit(ExitType.ALREADY, "already exited"));
				return;
			}
			
			if (!isMonitor){
				// 将对方加入自己的link列表
				addLink(aid);
			}
			// 将自己加入对方link列表
			ax.addLink(selfAid);
			// 给自己发送监视成功的消息
			axs.send(aid, selfAid, type);
		}
	}
	
	private void sendMessage(ActorId recvAid, String type, Message msg){
		msg.setSender(selfAid);
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
	
	private void addMessage(Message msg){
		msgQue.put(msg);
	}
	
	private MessageGuard recvMessage(Pattern pattern){
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
				return returnMessage(msg);
			}
		}
		
		long currTimeout = timeUnit.toMillis(timeout);
		while (true){
			long bt = 0;
			long eclipse = 0;
			if (currTimeout > 0 && currTimeout < Long.MAX_VALUE){
				bt = System.currentTimeMillis();
			}
			
			msg = msgQue.poll(currTimeout, timeUnit);
			if (msg == null){
				break;
			}
			
			msg = Mailbox.filter(msg, recvFilters);
			if (msg == null){
				continue;
			}
			
			boolean typesEmpty = ContainerUtils.isEmpty(matchedTypes);
			boolean actorsEmpty = ContainerUtils.isEmpty(matchedActors);
			if (typesEmpty && actorsEmpty){
				break;
			}
			
			if (bt > 0){
				eclipse = System.currentTimeMillis() - bt;
				if (eclipse < currTimeout){
					currTimeout -= eclipse;
				}else{
					currTimeout = 0;
				}
			}
			
			boolean found = Mailbox.match(msg, matchedTypes, matchedActors);
			if (found){
				break;
			}else{
				Mailbox mailbox = getMailbox();
				mailbox.add(msg);
			}
			
			if (currTimeout == 0){
				break;
			}
		}
		
		return returnMessage(msg);
	}
	
	private Packet recvMessage(Packet pkt, Pattern pattern){
		try (MessageGuard guard = recvMessage(pattern)){
			Message msg = guard.get();
			if (msg == null){
				return null;
			}
			
			return msg.move(pkt);
		}catch (Throwable e){
			return null;
		}
	}
	
	private MessageGuard returnMessage(Message msg){
		return msgGuardPool.get().wrap(msg);
	}
	
	private Message makeNewMessage(){
		if (handler != null){
			return Message.make(msgPool);
		}else{
			return Message.make();
		}
	}
	
	private Message makeMessage(Message src){
		if (handler != null){
			if (src != null){
				return Message.make(src, msgPool);
			}else{
				return Message.make(msgPool);
			}
		}else{
			if (src != null){
				return Message.make(src);
			}else{
				return Message.make();
			}
		}
	}
	
	private Set<ActorId> getLinkList(){
		if (linkList == null){
			linkList = new HashSet<ActorId>();
		}
		return linkList;
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
}
