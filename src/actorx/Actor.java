/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;
import actorx.util.MpscQueue;

/**
 * @author Xiong
 */
public class Actor {
	private ActorId aid = null;
	/**自己退出时需要发送EXIT消息的列表*/
	private List<ActorId> linkList = new ArrayList<ActorId>(1);
	/**消息队列*/
	private MpscQueue<Message> msgQue = new MpscQueue<Message>();
	/**邮箱*/
	private Mailbox mailbox = new Mailbox();
	/**临时数据，用于取消息时，匹配的消息类型列表*/
	private List<String> matchedTypes = new ArrayList<String>(5);
	/**是否已经结束*/
	private boolean quited = false;
	
	public Actor(ActorId aid){
		this.aid = aid;
	}
	
	/**
	 * 发送一个空的消息
	 * @param aid
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public void send(ActorId aid) throws AlreadyQuitedException{
		if (isQuited()){
			throw new AlreadyQuitedException();
		}
		
		sendMessage(this.getActorId(), aid, null);
	}
	
	/**
	 * 发送消息
	 * @param aid
	 * @param type 可以为null
	 * @param args 可以无参数
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public void send(ActorId aid, String type, Object... args) throws AlreadyQuitedException{
		if (isQuited()){
			throw new AlreadyQuitedException();
		}
		
		sendMessage(this.getActorId(), aid, type, args);
	}
	
	/**
	 * 转发一个消息；被转发的消息的sender会保持原始的sender
	 * @param aid
	 * @param msg
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public void relay(ActorId aid, Message msg) throws AlreadyQuitedException{
		if (isQuited()){
			throw new AlreadyQuitedException();
		}
		
		relayMessage(aid, msg);
	}
	
	/**
	 * 匹配指定的消息
	 * @param types 指定的消息中有一个匹配就返回；匹配顺序按照列表的自然顺序（从开头到结尾）
	 * @return
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public Actor match(String... types) throws AlreadyQuitedException{
		if (isQuited()){
			throw new AlreadyQuitedException();
		}
		
		for (String type : types){
			matchedTypes.add(type);
		}
		return this;
	}
	
	/**
	 * 阻塞当前线程等待至少有一个消息返回
	 * @return
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public Message recv() throws AlreadyQuitedException{
		return recv(Long.MAX_VALUE);
	}
	
	/**
	 * 阻塞当前线程等待至少有一个消息或者超时返回
	 * @param timeout 超时时间（毫秒）
	 * @return
	 * @throws AlreadyQuitedException 假如已经退出，抛出异常
	 */
	public Message recv(long timeout) throws AlreadyQuitedException{
		if (isQuited()){
			throw new AlreadyQuitedException();
		}
		
		Message msg = mailbox.fetch(matchedTypes);
		if (msg != null){
			clearRecvMeta();
			return msg;
		}
		
		long currTimeout = timeout;
		while (true){
			long bt = 0;
			long eclipse = 0;
			if (currTimeout > 0 && currTimeout < Long.MAX_VALUE){
				bt = System.currentTimeMillis();
			}
			
			msg = msgQue.poll(currTimeout);
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
		ctx.removeActor(aid);
		
		for (ActorId aid : linkList){
			sendMessage(this.getActorId(), aid, MessageType.EXIT, et, errmsg);
		}
		quited = true;
		mailbox.clear();
	}
	
	/**
	 * 取得自己的ActorId
	 * @return
	 */
	public ActorId getActorId(){
		return aid;
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
	 * @param args 参数，可以不指定
	 */
	public static void sendMessage(ActorId sender, ActorId recver, String type, Object... args){
		Actor a = Context.getInstance().getActor(recver);
		if (a == null){
			return;
		}
		a.addMessage(new Message(sender, type, args));
	}
	
	/**
	 * 发送一个空消息
	 * @param sender
	 * @param recver
	 */
	public static void sendMessage(ActorId sender, ActorId recver){
		sendMessage(sender, recver, null);
	}

	/**
	 * 使用空的sender发送消息
	 * @param recver
	 * @param type
	 * @param args
	 */
	public static void sendMessage(ActorId recver, String type, Object... args){
		sendMessage(null, recver, type, args);
	}
	
	/**
	 * 使用空的sender发送一个空的消息
	 * @param recver
	 */
	public static void sendMessage(ActorId recver){
		sendMessage(null, recver, null);
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
