/**
 * 
 */
package actorx.adl;

import adata.Stream;

/**
 * @author Xiong
 * 适配adl的接口
 */
public interface IAdlAdapter {
	void read(Stream stream);
	void write(Stream stream);
	int sizeOf();
}
