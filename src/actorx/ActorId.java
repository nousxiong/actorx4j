/**
 * 
 */
package actorx;

/**
 * @author Xiong
 */
public class ActorId {
	public static final ActorId NULL = null;
	
	/** 唯一id */
	private final long id;
	
	/** Context的时间戳 */
	private final long timestamp;
	
	public ActorId(long id, long timestamp){
		this.id = id;
		this.timestamp = timestamp;
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
			this.getId() == rhs.getId() && 
			this.getTimestamp() == rhs.getTimestamp();
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
		result = 37 * result + (int) (id ^ (id >>> 32));
		result = 37 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	public long getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
