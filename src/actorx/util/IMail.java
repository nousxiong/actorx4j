/**
 * 
 */
package actorx.util;

import actorx.ActorId;

/**
 * @author Xiong
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
	/// sender total list
	///------------------------------------------------------------------------
	public void setSenderTotalNext(IMail next);
	public IMail getSenderTotalNext();
	public void setSenderTotalPrev(IMail prev);
	public IMail getSenderTotalPrev();

	///------------------------------------------------------------------------
	/// sender type list
	///------------------------------------------------------------------------
	public void setSenderTypeNext(IMail next);
	public IMail getSenderTypeNext();
	public void setSenderTypePrev(IMail prev);
	public IMail getSenderTypePrev();

	public void setSenderTypeSameNext(IMail next);
	public IMail getSenderTypeSameNext();
	public void setSenderTypeSamePrev(IMail prev);
	public IMail getSenderTypeSamePrev();

	///------------------------------------------------------------------------
	/// type & sender
	///------------------------------------------------------------------------
	public String getType();
	public ActorId getSender();
	
	// 在SenderMailList.clear调用的时候，自己被从链表中移除后调用
	public void onClear();
}
