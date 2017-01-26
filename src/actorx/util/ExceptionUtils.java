/**
 * 
 */
package actorx.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Xiong
 * @creation 2017年1月26日下午12:07:36
 *
 */
public final class ExceptionUtils {
	
	/**
	 * 将异常堆栈信息输出到字符串中
	 * @param e
	 * @return
	 */
	public static String printStackTrace(Throwable e){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String str = sw.toString();
		pw.close();
		return str;
	}
	
}
