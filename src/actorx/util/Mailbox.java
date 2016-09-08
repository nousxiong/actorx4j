/**
 * 
 */
package actorx.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cque.SimpleNodePool;
import actorx.ActorId;
import actorx.Message;

/**
 * @author Xiong
 * actor的邮箱
 */
public class Mailbox {
	private IMail totalList;
	private IMail typeList;
	private Map<ActorId, SenderMailList> senderMails;
	private SimpleNodePool<SenderMailList> pool;
	private List<String> senderMatchedTypes;
	
	public void add(Message msg){
		addMail(msg);
		ActorId sender = msg.getSender();
		Map<ActorId, SenderMailList> senderMails = getSenderMails();
		SenderMailList senderMailList = senderMails.get(sender);
		if (senderMailList == null){
			senderMailList = makeSenderMailList();
			senderMails.put(sender, senderMailList);
		}
		senderMailList.addMail(msg);
	}
	
	public IMail fetch(List<String> matchedTypes, List<Object> matchedActors){
		boolean typesEmpty = CollectionUtils.isEmpty(matchedTypes);
		boolean actorsEmpty = CollectionUtils.isEmpty(matchedActors);
		if (typesEmpty && actorsEmpty){
			// 从totalList中pop一个消息
			return fetchMail();
		}
		
		if (!typesEmpty && typeList != null){
			IMail mail = fetchMail(matchedTypes);
			if (mail != null){
				return mail;
			}
		}

		Map<ActorId, SenderMailList> senderMails = getSenderMails();
		if (!actorsEmpty && !senderMails.isEmpty()){
			List<String> senderMatchedTypes = getSenderMatchedTypes();
			
			// 从senderMails中查找并移除指定actor和类型的消息
			ActorId matchedActor = null;
			for (Object obj : matchedActors){
				if (obj instanceof ActorId){
					if (matchedActor != null){
						IMail mail = fetchMatchedActorMail(matchedActor, senderMatchedTypes);
						if (mail != null){
							return mail;
						}
					}
					matchedActor = (ActorId) obj;
				}else if (obj instanceof String){
					senderMatchedTypes.add((String) obj);
				}
			}
			
			if (matchedActor != null){
				IMail mail = fetchMatchedActorMail(matchedActor, senderMatchedTypes);
				if (mail != null){
					return mail;
				}
			}
		}
		
		return null;
	}
	
	public boolean isEmpty(){
		return totalList == null;
	}
	
	public void clear(){
		if (totalList != null){
			IMail root = totalList;
			IMail itr = root;
			do{
				IMail mail = itr;
				itr = itr.getTotalNext();
				mail.onClear();
			}while (itr != null && itr != root);
		}
		totalList = null;
		typeList = null;
		
		if (senderMails != null && !senderMails.isEmpty()){
			for (SenderMailList senderMailList : senderMails.values()){
				senderMailList.clear();
				senderMailList.release();
			}
			senderMails.clear();
		}
	}
	
	private IMail fetchMatchedActorMail(ActorId sender, List<String> matchedTypes){
		try{
			// 查找指定的sender
			Map<ActorId, SenderMailList> senderMails = getSenderMails();
			SenderMailList senderMailList = senderMails.get(sender);
			if (senderMailList == null || senderMailList.isEmpty()){
				return null;
			}
			
			if (CollectionUtils.isEmpty(matchedTypes)){
				// 从senderMailList的totalList中pop一个消息
				IMail mail = senderMailList.fetchMail();
				removeMail(mail);
				return mail;
			}
			
			// 从senderMailList的typeList中查找并移除指定类型的消息
			IMail mail = senderMailList.fetchMail(matchedTypes);
			removeMail(mail);
			return mail;
		}finally{
			if (matchedTypes != null){
				matchedTypes.clear();
			}
		}
	}
	
	private void addMail(IMail mail){
		if (totalList == null && typeList == null){
			totalList = mail;
			typeList = mail;
			return;
		}
		
		// 加入totalList
		IMail tail = totalList.getTotalPrev();
		if (tail == null){
			mail.setTotalNext(totalList);
			mail.setTotalPrev(totalList);
			totalList.setTotalNext(mail);
			totalList.setTotalPrev(mail);
		}else{
			tail.setTotalNext(mail);
			mail.setTotalPrev(tail);
			totalList.setTotalPrev(mail);
			mail.setTotalNext(totalList);
		}
		
		// 加入typeList
		IMail typeSameRoot = getTypeMail(typeList, mail.getType());
		if (typeSameRoot == null){
			// 作为新的type加入typeList
			IMail typeTail = typeList.getTypePrev();
			if (typeTail == null){
				mail.setTypeNext(typeList);
				mail.setTypePrev(typeList);
				typeList.setTypeNext(mail);
				typeList.setTypePrev(mail);
			}else{
				typeTail.setTypeNext(mail);
				mail.setTypePrev(typeTail);
				typeList.setTypePrev(mail);
				mail.setTypeNext(typeList);
			}
		}else{
			// 作为已经存在的type加入TypeSameList
			IMail typeSameTail = typeSameRoot.getTypeSamePrev();
			if (typeSameTail == null){
				mail.setTypeSameNext(typeSameRoot);
				mail.setTypeSamePrev(typeSameRoot);
				typeSameRoot.setTypeSameNext(mail);
				typeSameRoot.setTypeSamePrev(mail);
			}else{
				typeSameTail.setTypeSameNext(mail);
				mail.setTypeSamePrev(typeSameTail);
				typeSameRoot.setTypeSamePrev(mail);
				mail.setTypeSameNext(typeSameRoot);
			}
		}
	}
	
	private IMail fetchMail(){
		if (totalList == null){
			return null;
		}
		
		IMail mail = totalList;
		removeMail(mail);
		removeSenderMail(mail);
		return mail;
	}
	
	private IMail fetchMail(List<String> matchedTypes){
		if (typeList == null){
			return null;
		}

		Map<ActorId, SenderMailList> senderMails = getSenderMails();
		IMail root = typeList;
		IMail itr = root;
		do{
			for (String type : matchedTypes){
				if (TypeComparator.compare(itr.getType(), type) == 0){
					totalList = removeTotalMail(totalList, itr);
					typeList = removeTypeMail(typeList, itr, itr);
					
					ActorId sender = itr.getSender();
					SenderMailList senderMailList = senderMails.get(sender);
					senderMailList.removeMail(itr);
					return itr;
				}
			}
			itr = itr.getTypeNext();
		}while (itr != null && itr != root);
		return null;
	}
	
	private void removeMail(IMail mail){
		if (mail == null){
			return;
		}
		
		totalList = removeTotalMail(totalList, mail);
		// 从typeList中移除
		// 找到同类消息列表的根节点
		IMail typeSameRoot = getTypeMail(typeList, mail.getType());
		if (typeSameRoot == null){
			return;
		}
		typeList = removeTypeMail(typeList, typeSameRoot, mail);
	}
	
	private static IMail getTypeMail(IMail root, String type){
		if (root == null){
			return null;
		}
		
		IMail itr = root;
		do{
			if (TypeComparator.compare(itr.getType(), type) == 0){
				return itr;
			}
			itr = itr.getTypeNext();
		}while (itr != null && itr != root);
		return null;
	}
	
	private void removeSenderMail(IMail mail){
		if (mail == null){
			return;
		}
		
		Map<ActorId, SenderMailList> senderMails = getSenderMails();
		ActorId sender = mail.getSender();
		SenderMailList senderMailList = senderMails.get(sender);
		senderMailList.removeMail(mail);
		if (senderMailList.isEmpty()){
			senderMails.remove(sender);
			senderMailList.release();
		}
	}
	
	/**
	 * 从totalList中移除mail
	 * @param root 不能为null
	 * @param mail 不能为null
	 * @return 返回totalList的root
	 */
	private static IMail removeTotalMail(IMail root, IMail mail){
		IMail prev = mail.getTotalPrev();
		IMail next = mail.getTotalNext();
		
		if (prev != null){
			if (prev == next){
				prev.setTotalNext(null);
			}else{
				prev.setTotalNext(next);
			}
		}
		
		if (next != null){
			if (prev == next){
				next.setTotalPrev(null);
			}else{
				next.setTotalPrev(prev);
			}
		}
		
		if (root == mail){
			root = next;
		}

		mail.setTotalPrev(null);
		mail.setTotalNext(null);
		return root;
	}
	
	/**
	 * 从typeList中移除mail
	 * @param root 不能为null
	 * @param mail 不能为null
	 * @return 返回typeList的root
	 */
	private static IMail removeTypeMail(IMail root, IMail typeSameRoot, IMail mail){
		// 移除mail
		IMail newTypeSameRoot = removeTypeSameMail(typeSameRoot, mail);
		if (newTypeSameRoot != typeSameRoot){
			// 将typeSameList（就是mail）从Type列表中移除，并指定newTypeSameRoot作为typeSameList
			IMail prev = typeSameRoot.getTypePrev();
			IMail next = typeSameRoot.getTypeNext();
			
			if (newTypeSameRoot == null){
				// 移除
				if (prev != null){
					if (prev == next){
						prev.setTypeNext(null);
					}else{
						prev.setTypeNext(next);
					}
				}
				
				if (next != null){
					if (prev == next){
						next.setTypePrev(null);
					}else{
						next.setTypePrev(prev);
					}
				}
				
				if (root == typeSameRoot){
					root = next;
				}
			}else{
				// 替换
				if (prev != null){
					prev.setTypeNext(newTypeSameRoot);
				}
				newTypeSameRoot.setTypePrev(prev);
				
				if (next != null){
					next.setTypePrev(newTypeSameRoot);
				}
				newTypeSameRoot.setTypeNext(next);
				
				if (root == typeSameRoot){
					root = newTypeSameRoot;
				}
			}
			typeSameRoot.setTypePrev(null);
			typeSameRoot.setTypeNext(null);
		}
		
		return root;
	}
	
	private static IMail removeTypeSameMail(IMail root, IMail mail){
		IMail prev = mail.getTypeSamePrev();
		IMail next = mail.getTypeSameNext();
		
		if (prev != null){
			if (prev == next){
				prev.setTypeSameNext(null);
			}else{
				prev.setTypeSameNext(next);
			}
		}
		
		if (next != null){
			if (prev == next){
				next.setTypeSamePrev(null);
			}else{
				next.setTypeSamePrev(prev);
			}
		}
		
		if (root == mail){
			root = next;
		}
		
		mail.setTypeSamePrev(null);
		mail.setTypeSameNext(null);
		return root;
	}
	
	private List<String> getSenderMatchedTypes(){
		if (senderMatchedTypes == null){
			senderMatchedTypes = new ArrayList<String>();
		}else{
			senderMatchedTypes.clear();
		}
		return senderMatchedTypes;
	}
	
	private Map<ActorId, SenderMailList> getSenderMails(){
		if (senderMails == null){
			senderMails = new HashMap<ActorId, SenderMailList>();
		}
		return senderMails;
	}
	
	private SenderMailList makeSenderMailList(){
		SimpleNodePool<SenderMailList> pool = getPool();
		return pool.get();
	}
	
	private SimpleNodePool<SenderMailList> getPool(){
		if (pool == null){
			pool = new SimpleNodePool<SenderMailList>(new SenderMailListFactory());
		}
		return pool;
	}
}
