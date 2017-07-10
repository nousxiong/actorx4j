/**
 * 
 */
package actorx;

import adata.Stream;

/**
 * @author Xiong
 *
 */
public abstract class AbstractCustomMessage extends Message {
	private String custemType;
	
	public AbstractCustomMessage(String custemType){
		super();
		this.custemType = custemType;
	}

	@Override
	public final void read(Stream stream){
		super.read(stream);
		cread(stream);
	}
	
	@Override
	public final void write(Stream stream){
		super.write(stream);
		cwrite(stream);
	}
	
	@Override
	public final int sizeOf(){
		return super.sizeOf() + csizeOf();
	}
	
	public String ctype(){
		return custemType;
	}
	
	public abstract void cread(Stream stream);
	public abstract void cwrite(Stream stream);
	public abstract int csizeOf();
	
	protected void cassign(AbstractCustomMessage src){
		throw new UnsupportedOperationException();
	}
	
	public void copy(AbstractCustomMessage src){
		super.copy(src);
		this.custemType = src.custemType;
		this.cassign(src);
	}
}
