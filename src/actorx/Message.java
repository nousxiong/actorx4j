/**
 * 
 */
package actorx;

import actorx.detail.IMail;
import cque.IFreer;
import cque.INode;

/**
 * @author Xiong
 */
public class Message implements INode, IMail {
	private ActorId sender;
	private String type;
	private Object[] args;

	public Message(){
	}
	
	public Message(int size){
		reserve(size);
	}
	
	public Message(ActorId sender){
		this.sender = sender;
	}
	
	public Message(ActorId sender, String type){
		this.sender = sender;
		this.type = type;
	}
	
	public Message(ActorId sender, String type, Object... args){
		this.sender = sender;
		this.type = type;
		this.args = args;
	}
	
	public ActorId getSender(){
		return sender;
	}
	
	public void setSender(ActorId sender){
		this.sender = sender;
	}
	
	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(int i){
		if (i >= args.length){
			return null;
		}
		
		return (T) args[i];
	}
	
	public Object[] get(){
		return args;
	}
	
	public void set(Object... args){
		this.args = args;
	}
	
	public void set(int i, Object arg){
		args[i] = arg;
	}
	
	public void reserve(int size){
		if (args == null){
			args = new Object[size];
		}else{
			if (size <= args.length){
				return;
			}
			Object[] newArgs = new Object[size];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			args = newArgs;
		}
	}
	
	/** 以下实现INode接口 */
	private INode next;
	private IFreer freer;

	@Override
	public void release(){
		if (freer != null){
			freer.free(this);
		}
	}

	@Override
	public INode getNext(){
		return next;
	}

	@Override
	public INode fetchNext(){
		INode nx = next;
		next = null;
		return nx;
	}

	@Override
	public void onFree(){
		sender = null;
		type = null;
		next = null;
		freer = null;
		
		totalNext = null;
		totalPrev = null;
		typeNext = null;
		typePrev = null;
		typeSameNext = null;
		typeSamePrev = null;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.next = null;
	}

	@Override
	public void setNext(INode next){
		this.next = next;
	}
	
	/** 以下实现IMail接口 */
	private IMail totalNext;
	private IMail totalPrev;
	private IMail typeNext;
	private IMail typePrev;
	private IMail typeSameNext;
	private IMail typeSamePrev;
	
	@Override
	public void setTotalNext(IMail next) {
		totalNext = next;
	}

	@Override
	public IMail getTotalNext() {
		return totalNext;
	}

	@Override
	public void setTotalPrev(IMail prev) {
		totalPrev = prev;
	}

	@Override
	public IMail getTotalPrev() {
		return totalPrev;
	}

	@Override
	public void setTypeNext(IMail next) {
		typeNext = next;
	}

	@Override
	public IMail getTypeNext() {
		return typeNext;
	}

	@Override
	public void setTypePrev(IMail prev) {
		typePrev = prev;
	}

	@Override
	public IMail getTypePrev() {
		return typePrev;
	}

	@Override
	public void setTypeSameNext(IMail next) {
		typeSameNext = next;
	}

	@Override
	public IMail getTypeSameNext() {
		return typeSameNext;
	}

	@Override
	public void setTypeSamePrev(IMail prev) {
		typeSamePrev = prev;
	}

	@Override
	public IMail getTypeSamePrev() {
		return typeSamePrev;
	}

	@Override
	public void onClear() {
		this.release();
	}
}
