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
	
	private long timeout = DEFAULT_TIMEOUT;
	private TimeUnit timeUnit = DEFAULT_TIMEUNIT;
	private List<String> matchedTypes;
	
	
	public Pattern match(String type){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(5);
		}
		matchedTypes.add(type);
		return this;
	}
	
	public Pattern match(String type1, String type2){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(5);
		}
		matchedTypes.add(type1);
		matchedTypes.add(type2);
		return this;
	}
	
	public Pattern match(String... types){
		if (matchedTypes == null){
			matchedTypes = new ArrayList<String>(5);
		}
		for (String type : types){
			matchedTypes.add(type);
		}
		return this;
	}
	
	public Pattern after(long timeout){
		return after(timeout, TimeUnit.MILLISECONDS);
	}
	
	public Pattern after(long timeout, TimeUnit timeUnit){
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		return this;
	}

	public long getTimeout() {
		return timeout;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public List<String> getMatchedTypes() {
		return matchedTypes;
	}

	public void clear(){
		timeout = Long.MAX_VALUE;
		timeUnit = TimeUnit.MILLISECONDS;
		if (matchedTypes == null){
			matchedTypes.clear();
		}
	}
}
