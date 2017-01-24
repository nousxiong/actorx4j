/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import actorx.detail.Mailbox;
import cque.IntrusiveMpscQueue;

/**
 * @author Xiong
 */
public class Actor {
	private ActorId selfAid = null;
	/**自己退出时需要发送EXIT消息的列表*/
	private List<ActorId> linkList = new ArrayList<ActorId>(1);
	/**消息队列*/
	private IntrusiveMpscQueue<Message> msgQue = new IntrusiveMpscQueue<Message>();
	/**邮箱*/
	private Mailbox mailbox = new Mailbox();
	/**临时数据，用于取消息时，匹配的消息类型列表*/
	private List<String> matchedTypes = new ArrayList<String>(5);
	/**是否已经结束*/
	private boolean quited = false;
	
	public Actor(ActorId aid){
		selfAid = aid;
	}
	
	/**
	 * 发送一个空的消息
	 * @param aid
	 */
	public void send(ActorId aid){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		
		sendMessage(selfAid, aid);
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type 可以为null
	 * @param args 可以无参数
	 */
	public void send(ActorId aid, String type, Object... args){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type, args);
	}
	
	public void send(ActorId aid, String type){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type);
	}
	
	public void send(ActorId aid, String type, Object arg){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type, arg);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type, arg, arg1);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1, Object arg2){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type, arg, arg1, arg2);
	}
	
	public void send(ActorId aid, String type, Object arg, Object arg1, Object arg2, Object arg3){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, type, arg, arg1, arg2, arg3);
	}
	
	/**
	 * 发送消息
	 * @param aid 接收者
	 * @param msg
	 */
	public void send(ActorId aid, Message msg){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg);
	}
	
	/**
	 * 发送消息
	 * @param aid 接收者
	 * @param msg
	 */
	public void send(ActorId aid, Message msg, String type, Object... args){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type, args);
	}
	
	public void send(ActorId aid, Message msg, String type){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type, arg);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type, arg, arg1);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1, Object arg2){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type, arg, arg1, arg2);
	}
	
	public void send(ActorId aid, Message msg, String type, Object arg, Object arg1, Object arg2, Object arg3){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		if (msg == null){
			throw new NullPointerException();
		}
		sendMessage(selfAid, aid, msg, type, arg, arg1, arg2, arg3);
	}
	
	/**
	 * 转发一个消息；被转发的消息的sender会保持原始的sender
	 * @param aid
	 * @param msg
	 */
	public void relay(ActorId aid, Message msg){
		if (isQuited()){
			throw new IllegalStateException();
		}
		if (aid == null){
			throw new NullPointerException();
		}
		relayMessage(aid, msg);
	}
	
	/**
	 * 匹配指定的消息
	 * @param types 指定的消息中有一个匹配就返回；匹配顺序按照列表的自然顺序（从开头到结尾）
	 * @return
	 */
	public Actor match(String... types){
		if (isQuited()){
			throw new IllegalStateException();
		}
		for (String type : types){
			matchedTypes.add(type);
		}
		return this;
	}
	
	public Actor match(String type){
		matchedTypes.add(type);
		return this;
	}
	
	public Actor match(String type, String type1){
		matchedTypes.add(type);
		matchedTypes.add(type1);
		return this;
	}
	
	public Actor match(String type, String type1, String type2){
		matchedTypes.add(type);
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		return this;
	}
	
	public Actor match(String type, String type1, String type2, String type3){
		matchedTypes.add(type);
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		matchedTypes.add(type3);
		return this;
	}
	
	/**
	 * 阻塞当前线程等待至少有一个消息返回
	 * @return
	 */
	public Message recv(){
		return recv(Long.MAX_VALUE);
	}
	
	/**
	 * 阻塞当前线程等待至少有一个消息或者超时返回
	 * @param timeout 超时时间（毫秒）
	 * @return
	 */
	public Message recv(long timeout){
		return recv(timeout, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 阻塞当前线程等待至少有一个消息或者超时返回
	 * @param timeout 超时时间
	 * @param tu
	 * @return
	 */
	public Message recv(long timeout, TimeUnit tu){
		assert !isQuited();
		
		Message msg = mailbox.fetch(matchedTypes);
		if (msg != null){
			clearRecvMeta();
			return msg;
		}
		
		timeout = tu.toMillis(timeout);
		long currTimeout = timeout;
		while (true){
			long bt = 0;
			long eclipse = 0;
			if (currTimeout > 0 && currTimeout < Long.MAX_VALUE){
				bt = System.currentTimeMillis();
			}
			
			msg = msgQue.poll(currTimeout, tu);
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
		
		clearRecvMeta();
		return msg;
	}

	/**
	 * 正常退出
	 */
	public void quit(){
		quit(ExitType.NORMAL, "no error");
	}

	/**
	 * 指定退出类型和可能的错误信息；可以调用多次，从第二次之后，就自动忽略
	 * @param et
	 * @param errmsg
	 */
	public void quit(ExitType et, String errmsg){
		if (isQuited()){
			return;
		}
		
		Context ctx = Context.getInstance();
		ctx.removeActor(selfAid);
		
		for (ActorId aid : linkList){
			send(aid, MessageType.EXIT, et, errmsg);
		}
		quited = true;
		mailbox.clear();
		msgQue.clear();
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
	 * @param msg 消息
	 */
	public static void sendMessage(ActorId recver, Message msg){
		Actor a = Context.getInstance().getActor(recver);
		if (a == null){
			return;
		}
		a.addMessage(msg);
	}
	
	/**
	 * 发送消息
	 * @param sender 发送者，可以为null
	 * @param recver 接收者，不能为null
	 * @param type 消息类型，可以为null
	 * @param args 参数，可以不指定
	 */
	public static void sendMessage(ActorId sender, ActorId recver, String type, Object... args){
		sendMessage(recver, new Message(sender, type, args));
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, String type){
		sendMessage(recver, new Message(sender, type));
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, String type, Object arg){
		Message msg = new Message(sender, type);
		msg.reserve(1);
		msg.set(0, arg);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, String type, Object arg, Object arg1){
		Message msg = new Message(sender, type);
		msg.reserve(2);
		msg.set(0, arg);
		msg.set(1, arg1);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, String type, Object arg, Object arg1, Object arg2){
		Message msg = new Message(sender, type);
		msg.reserve(3);
		msg.set(0, arg);
		msg.set(1, arg1);
		msg.set(2, arg2);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(
		ActorId sender, ActorId recver, String type, 
		Object arg, Object arg1, Object arg2, Object arg3
	){
		Message msg = new Message(sender, type);
		msg.reserve(4);
		msg.set(0, arg);
		msg.set(1, arg1);
		msg.set(2, arg2);
		msg.set(3, arg3);
		sendMessage(recver, msg);
	}
	
	/**
	 * 发送消息
	 * @param sender
	 * @param recver
	 * @param msg
	 * @param type
	 * @param args
	 */
	public static void sendMessage(ActorId sender, ActorId recver, Message msg, String type, Object... args){
		msg.setSender(sender);
		msg.setType(type);
		msg.set(args);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, Message msg, String type){
		msg.setSender(sender);
		msg.setType(type);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, Message msg, String type, Object arg){
		msg.setSender(sender);
		msg.setType(type);
		msg.reserve(1);
		msg.set(0, arg);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(ActorId sender, ActorId recver, Message msg, String type, Object arg, Object arg1){
		msg.setSender(sender);
		msg.setType(type);
		msg.reserve(2);
		msg.set(0, arg);
		msg.set(1, arg1);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(
		ActorId sender, ActorId recver, Message msg, String type, 
		Object arg, Object arg1, Object arg2
	){
		msg.setSender(sender);
		msg.setType(type);
		msg.reserve(3);
		msg.set(0, arg);
		msg.set(1, arg1);
		msg.set(2, arg2);
		sendMessage(recver, msg);
	}
	
	public static void sendMessage(
		ActorId sender, ActorId recver, Message msg, String type, 
		Object arg, Object arg1, Object arg2, Object arg3
	){
		msg.setSender(sender);
		msg.setType(type);
		msg.reserve(4);
		msg.set(0, arg);
		msg.set(1, arg1);
		msg.set(2, arg2);
		msg.set(3, arg3);
		sendMessage(recver, msg);
	}
	
	/**
	 * 发送一个空消息
	 * @param sender
	 * @param recver
	 */
	public static void sendMessage(ActorId sender, ActorId recver){
		sendMessage(sender, recver, (String) null);
	}
	
	/**
	 * 发送一个空消息
	 * @param sender
	 * @param recver
	 * @param msg
	 */
	public static void sendMessage(ActorId sender, ActorId recver, Message msg){
		sendMessage(sender, recver, msg, (String) null);
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
	
	private void clearRecvMeta(){
		matchedTypes.clear();
	}
	
	private static void relayMessage(ActorId aid, Message msg){
		Actor a = Context.getInstance().getActor(aid);
		if (a == null){
			return;
		}
		
		a.addMessage(msg);
	}
}
