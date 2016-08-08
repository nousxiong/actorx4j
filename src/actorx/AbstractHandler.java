/**
 * 
 */
package actorx;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Xiong
 * 基于线程的actor可执行体
 */
public abstract class AbstractHandler implements Runnable {
	private Actor self = null;
	@Override
	public void run(){
		assert self != null;
		ActorExit aex = new ActorExit(ExitType.NORMAL, "no error");
		try{
			self.init();
			run(self);
		}catch (Exception e){
			aex.setType(ExitType.EXCEPT);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			aex.setErrmsg(sw.toString());
			pw.close();
		}finally{
			self.quit(aex);
		}
	}

	abstract public void run(Actor self) throws Exception;

	public void setSelf(Actor self){
		this.self = self;
	}
}
