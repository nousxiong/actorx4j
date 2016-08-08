/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import actorx.util.CopyOnWriteBuffer;
import actorx.util.Mailbox;
import actorx.util.MessageGuardFactory;
import cque.IntrusiveMpscQueue;
import cque.MpscNodePool;
import cque.SimpleNodePool;

/**
 * @author Xiong
 */
public class Actor {
	// AxService
	private AxService axs;
	// ActorId
	private ActorId selfAid = null;
	// 是否有自己的handler
	private boolean handler = false;
	/**自己退出时需要发送EXIT消息的列表*/
	private List<ActorId> linkList = new ArrayList<ActorId>(1);
	/**消息队列*/
	private IntrusiveMpscQueue<Message> msgQue = new IntrusiveMpscQueue<Message>();
	/**邮箱*/
	private Mailbox mailbox = new Mailbox();
	// 消息守护者池
	private SimpleNodePool<MessageGuard> msgGuardPool = 
		new SimpleNodePool<MessageGuard>(new MessageGuardFactory());
	// 本地消息池
	private MpscNodePool<Message> msgPool;
	// 本地写时拷贝Buffer池
	private MpscNodePool<CopyOnWriteBuffer> cowBufferPool;
	/**临时数据，用于取消息时，匹配的消息类型列表*/
	private List<String> matchedTypes = new ArrayList<String>(5);
	/**是否已经结束*/
	private boolean quited = false;
	
	
	/**
	 * 创建Actor
	 * @param axs
	 * @param aid
	 */
	public Actor(AxService axs, ActorId aid, boolean handler){
		this.axs = axs;
		this.selfAid = aid;
		this.handler = handler;
	}
	
	/**
	 * 返回AxService
	 * @return
	 */
	public AxService getAxService(){
		return axs;
	}
	
	/**
	 * 创建消息
	 * @return
	 */
	public MessageGuard makeMessage(){
		return returnMessage(makeNewMessage());
	}
	
	/**
	 * 发送一个空消息
	 * @param aid
	 */
	public void send(ActorId aid){
		assert !isQuited();
		assert aid != null;
		
		Message msg = makeEmptyMessage();
		if (!sendMessage(axs, selfAid, aid, msg.getType(), msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type 可以为null
	 * @param arg 不可为null
	 */
	public <A> void send(ActorId aid, String type, A arg){
		assert !isQuited();
		assert aid != null;
		assert arg != null;
		
		Message msg = makeNewMessage();
		msg.put(arg);
		if (!sendMessage(axs, selfAid, aid, type, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type 可以为null
	 * @param arg1 不可为null
	 * @param arg2 不可为null
	 */
	public <A1, A2> void send(ActorId aid, String type, A1 arg1, A2 arg2){
		assert !isQuited();
		assert aid != null;
		
		Message msg = makeNewMessage();
		msg.put(arg1);
		msg.put(arg2);
		if (!sendMessage(axs, selfAid, aid, type, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type 可以为null
	 * @param arg1 不可为null
	 * @param arg2 不可为null
	 * @param arg3 不可为null
	 */
	public <A1, A2, A3> void send(ActorId aid, String type, A1 arg1, A2 arg2, A3 arg3){
		assert !isQuited();
		assert aid != null;
		
		Message msg = makeNewMessage();
		msg.put(arg1);
		msg.put(arg2);
		msg.put(arg3);
		if (!sendMessage(axs, selfAid, aid, type, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type
	 * @param args 可以为null
	 */
	public void send(ActorId aid, String type, Object... args){
		assert !isQuited();
		assert aid != null;
		
		Message msg = null;
		if (args == null){
			msg = makeEmptyMessage();
		}else{
			msg = makeMessage(null);
			for (Object arg : args){
				msg.put(arg);
			}
		}
		
		if (!sendMessage(axs, selfAid, aid, type, msg)){
			msg.release();
		}
	}
	
	/**
	 * 发送消息
	 * @param aid 接收者
	 * @param msg
	 */
	public void send(ActorId aid, Message src){
		assert !isQuited();
		assert aid != null;
		assert src != null;

		Message msg = makeMessage(src);
		msg.setSender(selfAid);
		if (!sendMessage(axs, aid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 转发一个消息；被转发的消息的sender会保持原始的sender
	 * @param aid
	 * @param msg
	 */
	public void relay(ActorId aid, Message src){
		assert !isQuited();
		assert aid != null;
		
		Message msg = makeMessage(src);
		if (!sendMessage(axs, aid, msg)){
			msg.release();
		}
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param type
	 * @return
	 */
	public MessageGuard recv(String type){
		matchedTypes.clear();
		matchedTypes.add(type);
		return recv(matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param type1
	 * @param type2
	 * @return
	 */
	public MessageGuard recv(String type1, String type2){
		matchedTypes.clear();
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		return recv(matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收指定匹配的消息
	 * @param types
	 * @return
	 */
	public MessageGuard recv(String... types){
		matchedTypes.clear();
		for (String type : types){
			matchedTypes.add(type);
		}
		return recv(matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收消息
	 * @return
	 */
	public MessageGuard recv(){
		matchedTypes.clear();
		return recv(matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @return
	 */
	public MessageGuard recv(long timeout){
		matchedTypes.clear();
		return recv(matchedTypes, timeout, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 接收指定超时时间的消息
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public MessageGuard recv(long timeout, TimeUnit timeUnit){
		matchedTypes.clear();
		return recv(matchedTypes, timeout, timeUnit);
	}
	
	/**
	 * 接收指定模式的消息
	 * @param patt
	 * @return
	 */
	public MessageGuard recv(Pattern patt){
		return recv(patt.getMatchedTypes(), patt.getTimeout(), patt.getTimeUnit());
	}

	///------------------------------------------------------------------------
	/// 使用Packet接收
	///------------------------------------------------------------------------
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type
	 * @return
	 */
	public Packet recv(Packet pkt, String type){
		matchedTypes.clear();
		matchedTypes.add(type);
		return recv(pkt, matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recv(Packet pkt, String type1, String type2){
		matchedTypes.clear();
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		return recv(pkt, matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param types
	 * @return
	 */
	public Packet recv(Packet pkt, String... types){
		matchedTypes.clear();
		for (String type : types){
			matchedTypes.add(type);
		}
		return recv(pkt, matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @return
	 */
	public Packet recv(Packet pkt){
		return recv(pkt, matchedTypes, Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @return
	 */
	public Packet recv(Packet pkt, long timeout){
		return recv(pkt, matchedTypes, timeout, Pattern.DEFAULT_TIMEUNIT);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public Packet recv(Packet pkt, long timeout, TimeUnit timeUnit){
		return recv(pkt, matchedTypes, timeout, timeUnit);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param patt
	 * @return
	 */
	public Packet recv(Packet pkt, Pattern patt){
		return recv(pkt, patt.getMatchedTypes(), Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}

	///------------------------------------------------------------------------
	/// 接收axExit消息
	///------------------------------------------------------------------------
	public ActorExit recvExit(){
		return recvExit(Pattern.DEFAULT_TIMEOUT, Pattern.DEFAULT_TIMEUNIT);
	}
	
	public ActorExit recvExit(long timeout){
		return recvExit(timeout, Pattern.DEFAULT_TIMEUNIT);
	}
	
	public ActorExit recvExit(long timeout, TimeUnit timeUnit){
		matchedTypes.clear();
		matchedTypes.add(MsgType.EXIT);
		try (MessageGuard guard = recv(matchedTypes, timeout, timeUnit)){
			Message msg = guard.get();
			if (msg == null){
				return null;
			}
			
			return msg.get(ActorExit.class);
		}catch (Exception e){
			return null;
		}
	}
	
	/**
	 * 运行Actor线程时，首先调用
	 */
	public void init(){
		if (handler){
			msgPool = MessagePool.getLocalPool();
			cowBufferPool = CowBufferPool.getLocalPool();
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
	public void quit(ActorExit aex){
		if (!axs.removeActor(selfAid)){
			return;
		}

		// 发送退出消息给所有链接的Actor
		if (!linkList.isEmpty()){
			for (ActorId aid : linkList){
				send(aid, MsgType.EXIT, aex);
			}
		}
		
		quited = true;
		mailbox.clear();
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
	public boolean isQuited(){
		return quited;
	}
	
	/**
	 * 发送消息
	 * @param sender 发送者，可以为null
	 * @param recver 接收者，不能为null
	 * @param type 消息类型，可以为null
	 * @param msg 消息
	 */
	public static boolean sendMessage(AxService axs, ActorId sender, ActorId recver, String type, Message msg){
		Actor a = axs.getActor(recver);
		if (a == null){
			return false;
		}
		
		msg.setSender(sender);
		msg.setType(type);
		a.addMessage(msg);
		return true;
	}
	
	/**
	 * 发送消息
	 * @param sender
	 * @param recver
	 * @param msg
	 */
	public static boolean sendMessage(AxService axs, ActorId recver, Message msg){
		Actor a = axs.getActor(recver);
		if (a == null){
			return false;
		}
		
		a.addMessage(msg);
		return true;
	}
	
	///------------------------------------------------------------------------
	/// 以下方法内部使用
	///------------------------------------------------------------------------
	public void addLink(ActorId target){
		linkList.add(target);
	}
	
	private void addMessage(Message msg){
		msgQue.put(msg);
	}
	
	private MessageGuard recv(List<String> matchedTypes, long timeout, TimeUnit timeUnit){
		assert !isQuited();
		
		if (matchedTypes != this.matchedTypes){
			this.matchedTypes.clear();
			if (matchedTypes != null){
				this.matchedTypes.addAll(matchedTypes);
			}
			matchedTypes = this.matchedTypes;
		}
		
		Message msg = mailbox.fetch(matchedTypes);
		if (msg != null){
			return returnMessage(msg);
		}
		
		timeout = timeUnit.toMillis(timeout);
		long currTimeout = timeout;
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
			
			// 整理msgQue，将有类型的消息分类放入类型队列
			if (matchedTypes.isEmpty()){
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
			
			boolean found = false;
			String msgType = msg.getType();
			for (String type : matchedTypes){
				if (type.equals(msgType)){
					found = true;
					break;
				}
			}
			
			if (found){
				break;
			}else{
				mailbox.add(msg);
			}
			
			if (currTimeout == 0){
				break;
			}
		}
		
		return returnMessage(msg);
	}
	
	private Packet recv(Packet pkt, List<String> matchedTypes, long timeout, TimeUnit timeUnit){
		try (MessageGuard guard = recv(matchedTypes, timeout, timeUnit)){
			Message msg = guard.get();
			if (msg == null){
				return null;
			}
			
			return msg.move(pkt);
		}catch (Exception e){
			return null;
		}
	}
	
	private MessageGuard returnMessage(Message msg){
		matchedTypes.clear();
		return msgGuardPool.get().wrap(msg);
	}
	
	private Message makeNewMessage(){
		if (handler){
			return Message.make(msgPool, cowBufferPool);
		}else{
			return Message.make();
		}
	}
	
	private Message makeMessage(Message src){
		if (handler){
			if (src != null){
				return Message.make(src, msgPool);
			}else{
				return Message.make(msgPool, cowBufferPool);
			}
		}else{
			if (src != null){
				return Message.make(src);
			}else{
				return Message.make();
			}
		}
	}
	
	private Message makeEmptyMessage(){
		if (handler){
			return Message.makeEmpty(msgPool);
		}else{
			return Message.makeEmpty();
		}
	}
}
