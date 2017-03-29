/**
 * 
 */
package actorx.detail;

import java.util.List;

import actorx.AtomCode;
import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 * 发送者邮件列表
 */
public class SenderMailList implements INode {
	private IMail totalList;
	private IMail typeList;
	
	public void addMail(IMail mail){
		if (totalList == null && typeList == null){
			totalList = mail;
			typeList = mail;
			return;
		}
		
		// 加入totalList
		IMail tail = totalList.getSenderTotalPrev();
		if (tail == null){
			mail.setSenderTotalNext(totalList);
			mail.setSenderTotalPrev(totalList);
			totalList.setSenderTotalNext(mail);
			totalList.setSenderTotalPrev(mail);
		}else{
			tail.setSenderTotalNext(mail);
			mail.setSenderTotalPrev(tail);
			totalList.setSenderTotalPrev(mail);
			mail.setSenderTotalNext(totalList);
		}
		
		// 加入typeList
		IMail typeSameRoot = getTypeMail(typeList, mail.getType());
		if (typeSameRoot == null){
			// 作为新的type加入typeList
			IMail typeTail = typeList.getSenderTypePrev();
			if (typeTail == null){
				mail.setSenderTypeNext(typeList);
				mail.setSenderTypePrev(typeList);
				typeList.setSenderTypeNext(mail);
				typeList.setSenderTypePrev(mail);
			}else{
				typeTail.setSenderTypeNext(mail);
				mail.setSenderTypePrev(typeTail);
				typeList.setSenderTypePrev(mail);
				mail.setSenderTypeNext(typeList);
			}
		}else{
			// 作为已经存在的type加入TypeSameList
			IMail typeSameTail = typeSameRoot.getSenderTypeSamePrev();
			if (typeSameTail == null){
				mail.setSenderTypeSameNext(typeSameRoot);
				mail.setSenderTypeSamePrev(typeSameRoot);
				typeSameRoot.setSenderTypeSameNext(mail);
				typeSameRoot.setSenderTypeSamePrev(mail);
			}else{
				typeSameTail.setSenderTypeSameNext(mail);
				mail.setSenderTypeSamePrev(typeSameTail);
				typeSameRoot.setSenderTypeSamePrev(mail);
				mail.setSenderTypeSameNext(typeSameRoot);
			}
		}
	}
	
	public IMail fetchMail(){
		if (totalList == null){
			return null;
		}
		
		IMail mail = totalList;
		removeMail(mail);
		return mail;
	}
	
	public IMail fetchMail(List<String> matchedTypes){
		if (typeList == null || matchedTypes.isEmpty()){
			return null;
		}
		
		IMail root = typeList;
		IMail itr = root;
		do{
			for (String type : matchedTypes){
				if (AtomCode.equals(itr.getType(), type)){
					totalList = removeTotalMail(totalList, itr);
					typeList = removeTypeMail(typeList, itr, itr);
					return itr;
				}
			}
			itr = itr.getSenderTypeNext();
		}while (itr != null && itr != root);
		return null;
	}
	
	public void removeMail(IMail mail){
		if (mail == null){
			return;
		}
		
		// 从totalList中移除
		totalList = removeTotalMail(totalList, mail);
		// 从typeList中移除
		// 找到同类消息列表的根节点
		IMail typeSameRoot = getTypeMail(typeList, mail.getType());
		if (typeSameRoot == null){
			return;
		}
		typeList = removeTypeMail(typeList, typeSameRoot, mail);
		
	}
	
	public void clear(){
		// 直接置空即可，因为外面Mailbox会从totalList依次调用IMail.onClear()
		totalList = null;
		typeList = null;
	}
	
	private static IMail removeTotalMail(IMail root, IMail mail){
		IMail prev = mail.getSenderTotalPrev();
		IMail next = mail.getSenderTotalNext();
		
		if (prev != null){
			if (prev == next){
				prev.setSenderTotalNext(null);
			}else{
				prev.setSenderTotalNext(next);
			}
		}
		
		if (next != null){
			if (prev == next){
				next.setSenderTotalPrev(null);
			}else{
				next.setSenderTotalPrev(prev);
			}
		}
		
		if (root == mail){
			root = next;
		}

		mail.setSenderTotalPrev(null);
		mail.setSenderTotalNext(null);
		return root;
	}
	
	private static IMail removeTypeMail(IMail root, IMail typeSameRoot, IMail mail){
		// 移除mail
		IMail newTypeSameRoot = removeTypeSameMail(typeSameRoot, mail);
		if (newTypeSameRoot != typeSameRoot){
			// 将typeSameList（就是mail）从Type列表中移除，并指定newTypeSameRoot作为typeSameList
			IMail prev = typeSameRoot.getSenderTypePrev();
			IMail next = typeSameRoot.getSenderTypeNext();
			
			if (newTypeSameRoot == null){
				// 移除
				if (prev != null){
					if (prev == next){
						prev.setSenderTypeNext(null);
					}else{
						prev.setSenderTypeNext(next);
					}
				}
				
				if (next != null){
					if (prev == next){
						next.setSenderTypePrev(null);
					}else{
						next.setSenderTypePrev(prev);
					}
				}
				
				if (root == typeSameRoot){
					root = next;
				}
			}else{
				// 替换
				if (prev != null){
					prev.setSenderTypeNext(newTypeSameRoot);
				}
				newTypeSameRoot.setSenderTypePrev(prev);
				
				if (next != null){
					next.setSenderTypePrev(newTypeSameRoot);
				}
				newTypeSameRoot.setSenderTypeNext(next);
				
				if (root == typeSameRoot){
					root = newTypeSameRoot;
				}
			}
			typeSameRoot.setSenderTypePrev(null);
			typeSameRoot.setSenderTypeNext(null);
		}
		
		return root;
	}
	
	private static IMail removeTypeSameMail(IMail root, IMail mail){
		IMail prev = mail.getSenderTypeSamePrev();
		IMail next = mail.getSenderTypeSameNext();
		
		if (prev != null){
			if (prev == next){
				prev.setSenderTypeSameNext(null);
			}else{
				prev.setSenderTypeSameNext(next);
			}
		}
		
		if (next != null){
			if (prev == next){
				next.setSenderTypeSamePrev(null);
			}else{
				next.setSenderTypeSamePrev(prev);
			}
		}
		
		if (root == mail){
			root = next;
		}
		
		mail.setSenderTypeSamePrev(null);
		mail.setSenderTypeSameNext(null);
		return root;
	}
	
	private static IMail getTypeMail(IMail root, String type){
		if (root == null){
			return null;
		}
		
		IMail itr = root;
		do{
			if (AtomCode.equals(itr.getType(), type)){
				return itr;
			}
			itr = itr.getSenderTypeNext();
		}while (itr != null && itr != root);
		return null;
	}
	
	public boolean isEmpty(){
		return totalList == null && typeList == null;
	}

	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;
	
	@Override
	public INode fetchNext() {
		INode nx = next;
		next = null;
		return nx;
	}

	@Override
	public INode getNext() {
		return next;
	}

	@Override
	public void onFree() {
		next = null;
		freer = null;
		
		totalList = null;
		typeList = null;
	}

	@Override
	public void onGet(IFreer freer) {
		this.freer = freer;
		this.next = null;
	}

	@Override
	public void release() {
		if (freer != null){
			freer.free(this);
		}
	}

	@Override
	public void setNext(INode next) {
		this.next = next;
	}
	
}
