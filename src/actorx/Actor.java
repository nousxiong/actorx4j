/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import actorx.util.CollectionUtils;
import actorx.util.IMail;
import actorx.util.Mailbox;
import actorx.util.MessageGuardFactory;
import actorx.util.TypeComparator;
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
	private List<ActorId> linkList;
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
		
		Message msg = makeNewMessage();
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
			msg = makeNewMessage();
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
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type
	 * @return
	 */
	public Packet recv(Packet pkt, String type){
		Pattern pattern = getPattern();
		pattern.match(type);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param type1
	 * @param type2
	 * @return
	 */
	public Packet recv(Packet pkt, String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(type1);
		pattern.match(type2);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param types
	 * @return
	 */
	public Packet recv(Packet pkt, String... types){
		Pattern pattern = getPattern();
		for (String type : types){
			pattern.match(type);
		}
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public Packet recv(Packet pkt, ActorId sender){
		Pattern pattern = getPattern();
		pattern.match(sender);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public Packet recv(Packet pkt, ActorId sender, String type){
		Pattern pattern = getPattern();
		pattern.match(sender, type);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public Packet recv(Packet pkt, ActorId sender, String type1, String type2){
		Pattern pattern = getPattern();
		pattern.match(sender, type1, type2);
		return recvMessage(pkt, pattern);
	}
	
	/**
	 * 接收指定匹配ActorId的消息
	 * @param sender
	 * @return
	 */
	public Packet recv(Packet pkt, ActorId sender, String... types){
		Pattern pattern = getPattern();
		pattern.match(sender, types);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @return
	 */
	public Packet recv(Packet pkt){
		Pattern pattern = getPattern();
		return recvMessage(pkt, pattern);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @return
	 */
	public Packet recv(Packet pkt, long timeout){
		Pattern pattern = getPattern();
		pattern.after(timeout);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 使用Packet接收指定超时时间的只读数据
	 * @param pkt
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	public Packet recv(Packet pkt, long timeout, TimeUnit timeUnit){
		Pattern pattern = getPattern();
		pattern.after(timeout, timeUnit);
		return recvMessage(pkt, pattern);
	}

	/**
	 * 使用Packet接收只读数据
	 * @param pkt 如果null，创建一个新Packet；反之，用户之前创建或者缓存的Packet
	 * @param pattern
	 * @return
	 */
	public Packet recv(Packet pkt, Pattern pattern){
		return recvMessage(pkt, pattern);
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
		Pattern pattern = getPattern();
		pattern.match(MsgType.EXIT);
		try (MessageGuard guard = recv(pattern)){
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
	public void quit(ActorExit aex){
		if (!axs.removeActor(selfAid)){
			return;
		}

		// 发送退出消息给所有链接的Actor
		if (!CollectionUtils.isEmpty(linkList)){
			for (ActorId aid : linkList){
				send(aid, MsgType.EXIT, aex);
			}
		}
		
		quited = true;
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
		List<ActorId> linkList = getLinkList();
		linkList.add(target);
	}
	
	private void addMessage(Message msg){
		msgQue.put(msg);
	}
	
	private MessageGuard recvMessage(Pattern pattern){
		assert !isQuited();
		
		List<String> matchedTypes = pattern.getMatchedTypes();
		List<Object> matchedActors = pattern.getMatchedActors();
		long timeout = pattern.getTimeout();
		TimeUnit timeUnit = pattern.getTimeUnit();
		
		Message msg = null;
		if (mailbox != null && !mailbox.isEmpty()){
			IMail mail = mailbox.fetch(matchedTypes, matchedActors);
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
			
			boolean typesEmpty = CollectionUtils.isEmpty(matchedTypes);
			boolean actorsEmpty = CollectionUtils.isEmpty(matchedActors);
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
			
			boolean found = false;
			if (!typesEmpty){
				String msgType = msg.getType();
				for (String type : matchedTypes){
					if (TypeComparator.compare(type, msgType) == 0){
						found = true;
						break;
					}
				}
			}
			
			if (!found && !actorsEmpty){
				String msgType = msg.getType();
				ActorId msgSender = msg.getSender();
				boolean matchedActor = false;
				boolean hasTypes = false;
				for (Object obj : matchedActors){
					if (obj instanceof ActorId){
						if (matchedActor && !hasTypes){
							found = true;
							break;
						}
						ActorId sender = (ActorId) obj;
						matchedActor = ActorId.compare(sender, msgSender) == 0;
						hasTypes = false;
					}else if (obj instanceof String){
						hasTypes = true;
						if (matchedActor){
							String type = (String) obj;
							if (TypeComparator.compare(type, msgType) == 0){
								found = true;
								break;
							}
						}
					}
				}

				if (matchedActor && !hasTypes){
					found = true;
				}
			}
			
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
		}catch (Exception e){
			return null;
		}
	}
	
	private MessageGuard returnMessage(Message msg){
		return msgGuardPool.get().wrap(msg);
	}
	
	private Message makeNewMessage(){
		if (handler){
			return Message.make(msgPool);
		}else{
			return Message.make();
		}
	}
	
	private Message makeMessage(Message src){
		if (handler){
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
	
	private List<ActorId> getLinkList(){
		if (linkList == null){
			linkList = new ArrayList<ActorId>(5);
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
}
