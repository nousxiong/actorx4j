/**
 * 
 */
package actorx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Xiong
 * 接收消息的模式
 */
public class Pattern {
	public static final long DEFAULT_TIMEOUT = Long.MAX_VALUE;
	public static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;
	
	private List<String> matchedTypes;
	private List<Object> matchedActors;
	private long timeout = DEFAULT_TIMEOUT;
	private TimeUnit timeUnit = DEFAULT_TIMEUNIT;
	
	
	public Pattern match(String type){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(1);
		}
		matchedTypes.add(type);
		return this;
	}
	
	public Pattern match(String type1, String type2){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(2);
		}
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		return this;
	}
	
	public Pattern match(String type1, String type2, String type3){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(3);
		}
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		matchedTypes.add(type3);
		return this;
	}
	
	public Pattern match(String... types){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(types.length);
		}
		for (String type : types){
			matchedTypes.add(type);
		}
		return this;
	}
	
	public Pattern match(ActorId aid){
		if (matchedActors == null){
			matchedActors = new ArrayList<Object>(1);
		}
		matchedActors.add(aid);
		return this;
	}
	
	public Pattern match(ActorId aid, String type){
		if (matchedActors == null){
			matchedActors = new ArrayList<Object>(2);
		}
		matchedActors.add(aid);
		matchedActors.add(type);
		return this;
	}
	
	public Pattern match(ActorId aid, String type1, String type2){
		if (matchedActors == null){
			matchedActors = new ArrayList<Object>(3);
		}
		matchedActors.add(aid);
		matchedActors.add(type1);
		matchedActors.add(type2);
		return this;
	}
	
	public Pattern match(ActorId aid, String type1, String type2, String type3){
		if (matchedActors == null){
			matchedActors = new ArrayList<Object>(4);
		}
		matchedActors.add(aid);
		matchedActors.add(type1);
		matchedActors.add(type2);
		matchedActors.add(type3);
		return this;
	}
	
	public Pattern match(ActorId aid, String... types){
		if (matchedActors == null){
			matchedActors = new ArrayList<Object>(1 + types.length);
		}
		matchedActors.add(aid);
		for (String type : types){
			matchedActors.add(type);
		}
		return this;
	}
	
	public Pattern after(long timeout){
		return after(timeout, DEFAULT_TIMEUNIT);
	}
	
	public Pattern after(long timeout, TimeUnit timeUnit){
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		return this;
	}
	
//	public void copyFrom(Pattern other){
//		if (other != null){
//			matchedTypes = CollectionUtils.copyTypes(other.matchedTypes, matchedTypes);
//			matchedActors = CollectionUtils.copySenders(other.matchedActors, matchedActors);
//			timeout = other.timeout;
//			timeUnit = other.timeUnit;
//		}else{
//			if (matchedTypes != null){
//				matchedTypes.clear();
//			}
//			if (matchedActors != null){
//				matchedActors.clear();
//			}
//			timeout = DEFAULT_TIMEOUT;
//			timeUnit = DEFAULT_TIMEUNIT;
//		}
//	}

	public List<String> getMatchedTypes() {
		return matchedTypes;
	}
	
	public List<Object> getMatchedActors(){
		return matchedActors;
	}

	public long getTimeout() {
		return timeout;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void clear(){
		if (matchedTypes != null){
			matchedTypes.clear();
		}
		if (matchedActors != null){
			matchedActors.clear();
		}
		timeout = Long.MAX_VALUE;
		timeUnit = DEFAULT_TIMEUNIT;
	}
}
