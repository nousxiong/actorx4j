/**
 * 
 */
package actorx.util;

import actorx.ActorId;

/**
 * @author Xiong
 * @creation 2017年1月24日下午4:30:54
 * 邮件接口
 */
public interface IMail {

	///------------------------------------------------------------------------
	/// total list
	///------------------------------------------------------------------------
	public void setTotalNext(IMail next);
	public IMail getTotalNext();
	public void setTotalPrev(IMail prev);
	public IMail getTotalPrev();

	///------------------------------------------------------------------------
	/// type list
	///------------------------------------------------------------------------
	public void setTypeNext(IMail next);
	public IMail getTypeNext();
	public void setTypePrev(IMail prev);
	public IMail getTypePrev();

	public void setTypeSameNext(IMail next);
	public IMail getTypeSameNext();
	public void setTypeSamePrev(IMail prev);
	public IMail getTypeSamePrev();

	///------------------------------------------------------------------------
	/// type & sender
	///------------------------------------------------------------------------
	public String getType();
	public ActorId getSender();
	
	// 在Mailbox.clear调用的时候，自己被从链表中移除后调用
	public void onClear();
}
