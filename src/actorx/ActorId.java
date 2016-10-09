/**
 * 
 */
package actorx;

/**
 * @author Xiong
 */
public class ActorId extends actorx.adl.ActorId {
	// 空ActorId
	public static final ActorId NULLAID = new ActorId(0, 0, 0, 0);
	// null ActorId
	public static final ActorId NULL = null;
	
	public ActorId(long axid, long timestamp, long id, long sid){
		this.axid = axid;
		this.timestamp = timestamp;
		this.id = id;
		this.sid = sid;
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o){
			return true;
		}
		
		if (o == null || getClass() != o.getClass()){
			return false;
		}
		
		ActorId rhs = (ActorId)o;
		return 
			this.getAxid() == rhs.getAxid() && 
			this.getTimestamp() == rhs.getTimestamp() && 
			this.getId() == rhs.getId() && 
			this.getSid() == rhs.getSid();
	}
	
	/**
		[1]把某个非零常数值（一般取素数），例如17，保存在int变量result中；
		[2]对于对象中每一个关键域f（指equals方法中考虑的每一个域）：
			[2.1]boolean型，计算(f ? 0 : 1);
			[2.2]byte,char,short型，计算(int)f;
			[2.3]long型，计算(int) (f ^ (f>>>32));
			[2.4]float型，计算Float.floatToIntBits(afloat);
			[2.5]double型，计算Double.doubleToLongBits(adouble)得到一个long，再执行[2.3];
			[2.6]对象引用，递归调用它的hashCode方法;
			[2.7]数组域，对其中每个元素调用它的hashCode方法。
		[3]将上面计算得到的散列码保存到int变量c，然后执行 result=37*result+c;
		[4]返回result
	 */
	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + (int) (axid ^ (axid >>> 32));
		result = 37 * result + (int) (timestamp ^ (timestamp >>> 32));
		result = 37 * result + (int) (id ^ (id >>> 32));
		result = 37 * result + (int) (sid ^ (sid >>> 32));
		return result;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder(32);
		sb.append("Aid<");
		sb.append(axid).append('.');
		sb.append(timestamp).append('.');
		sb.append(id).append('.');
		sb.append(sid).append(">");
		return sb.toString();
	}
	
	/**
	 * 比较2个ActorId
	 * @param lhs
	 * @param rhs
	 * @return 0 ==, -1 <, 1 >
	 */
	public static int compare(ActorId lhs, ActorId rhs){
		if (lhs != null && rhs == null){
			return 1;
		}else if (lhs == null && rhs != null){
			return -1;
		}else if (lhs == null && rhs == null){
			return 0;
		}
		
		if (lhs.axid < rhs.axid){
			return -1;
		}else if (lhs.axid > rhs.axid){
			return 1;
		}
		
		if (lhs.timestamp < rhs.timestamp){
			return -1;
		}else if (lhs.timestamp > rhs.timestamp){
			return 1;
		}
		
		if (lhs.id < rhs.id){
			return -1;
		}else if (lhs.id > rhs.id){
			return 1;
		}
		
		if (lhs.sid < rhs.sid){
			return -1;
		}else if (lhs.sid > rhs.sid){
			return 1;
		}
		
		return 0;
	}
	
	/**
	 * 比较两个ActorId是否相等
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static boolean equals(ActorId lhs, ActorId rhs){
		return compare(lhs, rhs) == 0;
	}
}
