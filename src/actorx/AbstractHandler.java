/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * 基于线程的actor可执行体
 */
public abstract class AbstractHandler implements Runnable {
	private Actor self = null;
	@Override
	public void run(){
		assert self != null;
		ExitType et = ExitType.NORMAL;
		String errmsg = "no error";
		try{
			run(self);
		}catch (Exception e){
			et = ExitType.EXCEPT;
			errmsg = e.getMessage();
		}finally{
			self.quit(et, errmsg);
		}
	}

	abstract public void run(Actor self);

	public void setSelf(Actor self){
		this.self = self;
	}
}
