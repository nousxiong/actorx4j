/**
 * 
 */
package actorx.util;

import java.util.List;

import actorx.Message;
import actorx.MessageType;

/**
 * @author Xiong
 * @creation 2017年1月24日下午4:39:19
 * actor的邮箱
 */
public class Mailbox {
	private IMail totalList;
	private IMail typeList;
	
	public void add(Message mail){
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
	
	/**
	 * 尝试获取指定类型列表的邮件
	 * @param matchedTypes
	 * @return 返回符合匹配的消息
	 */
	public Message fetch(List<String> matchedTypes){
		boolean typesEmpty = ContainerUtils.isEmpty(matchedTypes);
		if (typesEmpty){
			// 从totalList中pop一个消息
			return (Message) fetchMail();
		}
		
		if (!typesEmpty && typeList != null){
			return (Message) fetchMail(matchedTypes);
		}
		
		return null;
	}
	
	/**
	 * 清空邮箱
	 */
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
	}
	
	/**
	 * 是否空
	 * @return
	 */
	public boolean isEmpty(){
		return totalList == null;
	}
	
	private IMail fetchMail(){
		if (totalList == null){
			return null;
		}
		
		IMail mail = totalList;
		removeMail(mail);
		return mail;
	}
	
	private IMail fetchMail(List<String> matchedTypes){
		if (typeList == null){
			return null;
		}

		IMail root = typeList;
		IMail itr = root;
		do{
			for (String type : matchedTypes){
				if (MessageType.equals(itr.getType(), type)){
					totalList = removeTotalMail(totalList, itr);
					typeList = removeTypeMail(typeList, itr, itr);
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
			if (MessageType.equals(itr.getType(), type)){
				return itr;
			}
			itr = itr.getTypeNext();
		}while (itr != null && itr != root);
		return null;
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
}
