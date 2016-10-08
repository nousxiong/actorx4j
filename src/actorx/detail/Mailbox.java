/**
 * 
 */
package actorx.detail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cque.SimpleNodePool;
import actorx.ActorId;
import actorx.IRecvFilter;
import actorx.Message;
import actorx.MsgType;
import actorx.util.ContainerUtils;

/**
 * @author Xiong
 * actor的邮箱
 */
public class Mailbox {
	enum MailType {
		NORMAL,
		FILTERED
	}
	
	// 未过滤过的消息
	private IMail totalList;
	private IMail typeList;
	private Map<ActorId, SenderMailList> senderMails;
	// 过滤后暂时未被匹配的消息
	private IMail filteredTotalList;
	private IMail filteredTypeList;
	private Map<ActorId, SenderMailList> filteredSenderMails;
	
	private SimpleNodePool<SenderMailList> pool;
	private List<String> senderMatchedTypes;
	
	/**
	 * 按照分类加入消息
	 * @param msg
	 */
	public void add(Message msg){
		addMail(msg, MailType.NORMAL);
		ActorId sender = msg.getSender();
		Map<ActorId, SenderMailList> senderMails = getSenderMails();
		SenderMailList senderMailList = senderMails.get(sender);
		if (senderMailList == null){
			senderMailList = makeSenderMailList();
			senderMails.put(sender, senderMailList);
		}
		senderMailList.addMail(msg);
	}
	
	/**
	 * 尝试对执行的消息匹配指定的匹配类型和匹配Actor列表
	 * @param msg
	 * @param matchedTypes
	 * @param matchedActors
	 * @return true表示msg成功匹配，false表示msg不匹配
	 */
	public static boolean match(Message msg, List<String> matchedTypes, List<Object> matchedActors){
		boolean typesEmpty = ContainerUtils.isEmpty(matchedTypes);
		boolean actorsEmpty = ContainerUtils.isEmpty(matchedActors);
		if (typesEmpty && actorsEmpty){
			return true;
		}
		
		boolean found = false;
		if (!typesEmpty){
			String msgType = msg.getType();
			for (String type : matchedTypes){
				if (MsgType.equals(type, msgType)){
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
					matchedActor = ActorId.equals(sender, msgSender);
					hasTypes = false;
				}else if (obj instanceof String){
					hasTypes = true;
					if (matchedActor){
						String type = (String) obj;
						if (MsgType.compare(type, msgType) == 0){
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
		return found;
	}
	
	/**
	 * 获取指定过滤器的邮件并且进行回调过滤；无论是否过滤都会尝试匹配消息
	 * @param matchedTypes
	 * @param matchedActors
	 * @param mailFilters
	 * @return 返回符合匹配的消息
	 */
	public IMail fetch(List<String> matchedTypes, List<Object> matchedActors, Map<String, Set<IRecvFilter>> recvFilters){
		// 尝试匹配之前过滤过的消息
		if (!isFilteredEmpty()){
			IMail mail = fetch(matchedTypes, matchedActors, MailType.FILTERED);
			if (mail != null){
				return mail;
			}
		}
		
		// 如果没有过滤器，直接匹配邮箱中的消息
		if (ContainerUtils.isEmpty(recvFilters)){
			return fetch(matchedTypes, matchedActors, MailType.NORMAL);
		}
		
		// 遍历mailFilters中每种消息的过滤器
		for (Entry<String, Set<IRecvFilter>> recvFilterEntry : recvFilters.entrySet()){
			String type = recvFilterEntry.getKey();
			IMail mail = getTypeMail(typeList, type);
			while (mail != null){
				IMail next = null;
				if (hasNextTypeSameMail(mail, mail)){
					next = nextTypeSameMail(mail);
				}
				
				// 移除这个节点消息
				totalList = removeTotalMail(totalList, mail);
				typeList = removeTypeMail(typeList, mail, mail);
				removeSenderMail(mail);
				
				// 回调过滤器
				Message msg = (Message) mail;
				Set<IRecvFilter> recvFilterSet = recvFilterEntry.getValue();
				msg = filter(msg, recvFilterSet);

				if (msg != null){
					// 尝试匹配
					if (Mailbox.match(msg, matchedTypes, matchedActors)){
						return msg;
					}else{
						// 加入过滤后的邮箱
						addFiltered(msg);
					}
				}
				
				// 继续下一个
				mail = next;
			}
		}
		
		// 如果没有匹配到消息，最后尝试匹配邮箱中未过滤过的消息
		return fetch(matchedTypes, matchedActors, MailType.NORMAL);
	}
	
	/**
	 * 过滤指定的消息
	 * @param msg
	 * @param recvFilters
	 * @return
	 */
	public static Message filter(Message msg, Map<String, Set<IRecvFilter>> recvFilters){
		if (ContainerUtils.isEmpty(recvFilters)){
			return msg;
		}
		
		String type = msg.getType();
		Set<IRecvFilter> recvFilterSet = recvFilters.get(type);
		if (!ContainerUtils.isEmpty(recvFilterSet)){
			msg = filter(msg, recvFilterSet);
		}
		return msg;
	}
	
	/**
	 * 是否空
	 * @return
	 */
	public boolean isEmpty(){
		return totalList == null && isFilteredEmpty();
	}
	
	/**
	 * 清空邮箱
	 */
	public void clear(){
		// 清除普通消息
		clear(totalList, typeList, senderMails);
		totalList = null;
		typeList = null;
		
		// 清除过滤后的消息
		clear(filteredTotalList, filteredTypeList, filteredSenderMails);
		filteredTotalList = null;
		filteredTypeList = null;
	}
	
	private boolean isFilteredEmpty(){
		return filteredTotalList == null;
	}
	
	private static boolean hasNextTypeSameMail(IMail typeSameRoot, IMail curr){
		if (typeSameRoot == null){
			return false;
		}
		
		IMail next = curr.getTypeSameNext();
		if (next == null){
			return false;
		}
		
		if (next == typeSameRoot){
			return false;
		}
		return true;
	}
	
	private static IMail nextTypeSameMail(IMail curr){
		return curr.getTypeSameNext();
	}
	
	private void addFiltered(Message msg){
		addMail(msg, MailType.FILTERED);
		ActorId sender = msg.getSender();
		Map<ActorId, SenderMailList> senderMails = getFilteredSenderMails();
		SenderMailList senderMailList = senderMails.get(sender);
		if (senderMailList == null){
			senderMailList = makeSenderMailList();
			senderMails.put(sender, senderMailList);
		}
		senderMailList.addMail(msg);
	}
	
	private IMail fetch(List<String> matchedTypes, List<Object> matchedActors, MailType mailType){
		boolean isFiltered = mailType == MailType.FILTERED;
		IMail typeList = isFiltered ? filteredTypeList : this.typeList;
		boolean typesEmpty = ContainerUtils.isEmpty(matchedTypes);
		boolean actorsEmpty = ContainerUtils.isEmpty(matchedActors);
		if (typesEmpty && actorsEmpty){
			// 从totalList中pop一个消息
			if (isFiltered){
				return fetchFilteredMail();
			}else{
				return fetchMail();
			}
		}
		
		if (!typesEmpty && typeList != null){
			IMail mail = isFiltered ? fetchFilteredMail(matchedTypes) : fetchMail(matchedTypes);
			if (mail != null){
				return mail;
			}
		}

		Map<ActorId, SenderMailList> senderMails = isFiltered ? getFilteredSenderMails() : getSenderMails();
		if (!actorsEmpty && !senderMails.isEmpty()){
			List<String> senderMatchedTypes = getSenderMatchedTypes();
			
			// 从senderMails中查找并移除指定actor和类型的消息
			ActorId matchedActor = null;
			for (Object obj : matchedActors){
				if (obj instanceof ActorId){
					if (matchedActor != null){
						IMail mail = 
							isFiltered ? 
							fetchFilteredMatchedActorMail(matchedActor, senderMatchedTypes) :
							fetchMatchedActorMail(matchedActor, senderMatchedTypes);
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
				IMail mail = 
					isFiltered ? 
					fetchFilteredMatchedActorMail(matchedActor, senderMatchedTypes) :
					fetchMatchedActorMail(matchedActor, senderMatchedTypes);
				if (mail != null){
					return mail;
				}
			}
		}
		
		return null;
	}
	
	private static Message filter(Message msg, Set<IRecvFilter> recvFilterSet){
		String type = msg.getType();
		ActorId fromAid = msg.getSender();
		Message prevMsg = msg;
		
		// 回调过滤器
		for (IRecvFilter recvFilter : recvFilterSet){
			Message filteredMsg = recvFilter.filterRecv(fromAid, type, prevMsg, msg);
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
		return msg;
	}
	
	private static void clear(IMail totalList, IMail typeList, Map<ActorId, SenderMailList> senderMails){
		if (totalList != null){
			IMail root = totalList;
			IMail itr = root;
			do{
				IMail mail = itr;
				itr = itr.getTotalNext();
				mail.onClear();
			}while (itr != null && itr != root);
		}
		
		if (!ContainerUtils.isEmpty(senderMails)){
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
			
			if (ContainerUtils.isEmpty(matchedTypes)){
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
	
	private IMail fetchFilteredMatchedActorMail(ActorId sender, List<String> matchedTypes){
		try{
			// 查找指定的sender
			Map<ActorId, SenderMailList> senderMails = getFilteredSenderMails();
			SenderMailList senderMailList = senderMails.get(sender);
			if (senderMailList == null || senderMailList.isEmpty()){
				return null;
			}
			
			if (ContainerUtils.isEmpty(matchedTypes)){
				// 从senderMailList的totalList中pop一个消息
				IMail mail = senderMailList.fetchMail();
				removeFilteredMail(mail);
				return mail;
			}
			
			// 从senderMailList的typeList中查找并移除指定类型的消息
			IMail mail = senderMailList.fetchMail(matchedTypes);
			removeFilteredMail(mail);
			return mail;
		}finally{
			if (matchedTypes != null){
				matchedTypes.clear();
			}
		}
	}
	
	private void addMail(IMail mail, MailType mailType){
		boolean isFiltered = mailType == MailType.FILTERED;
		IMail totalList = isFiltered ? this.filteredTotalList : this.totalList;
		IMail typeList = isFiltered ? this.filteredTypeList : this.typeList;
		if (totalList == null && typeList == null){
			if (isFiltered){
				this.filteredTotalList = mail;
				this.filteredTypeList = mail;
			}else{
				this.totalList = mail;
				this.typeList = mail;
			}
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
	
	private IMail fetchFilteredMail(){
		if (filteredTotalList == null){
			return null;
		}
		
		IMail mail = filteredTotalList;
		removeFilteredMail(mail);
		removeFilteredSenderMail(mail);
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
				if (MsgType.equals(itr.getType(), type)){
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
	
	private IMail fetchFilteredMail(List<String> matchedTypes){
		if (filteredTypeList == null){
			return null;
		}

		Map<ActorId, SenderMailList> senderMails = getFilteredSenderMails();
		IMail root = filteredTypeList;
		IMail itr = root;
		do{
			for (String type : matchedTypes){
				if (MsgType.equals(itr.getType(), type)){
					filteredTotalList = removeTotalMail(filteredTotalList, itr);
					filteredTypeList = removeTypeMail(filteredTypeList, itr, itr);
					
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
	
	private void removeFilteredMail(IMail mail){
		if (mail == null){
			return;
		}
		
		filteredTotalList = removeTotalMail(filteredTotalList, mail);
		// 从typeList中移除
		// 找到同类消息列表的根节点
		IMail typeSameRoot = getTypeMail(filteredTypeList, mail.getType());
		if (typeSameRoot == null){
			return;
		}
		filteredTypeList = removeTypeMail(filteredTypeList, typeSameRoot, mail);
	}
	
	private static IMail getTypeMail(IMail root, String type){
		if (root == null){
			return null;
		}
		
		IMail itr = root;
		do{
			if (MsgType.equals(itr.getType(), type)){
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
	
	private void removeFilteredSenderMail(IMail mail){
		if (mail == null){
			return;
		}
		
		Map<ActorId, SenderMailList> senderMails = getFilteredSenderMails();
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
	
	private Map<ActorId, SenderMailList> getFilteredSenderMails(){
		if (filteredSenderMails == null){
			filteredSenderMails = new HashMap<ActorId, SenderMailList>();
		}
		return filteredSenderMails;
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
