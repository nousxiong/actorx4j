/**
 * 
 */
package actorx.util;

import java.net.URL;

/**
 * @author Xiong
 *
 */
public final class FileUtils {
	public static String getAbsolutePath(String relativePath){
		URL url = FileUtils.class.getClassLoader().getResource(relativePath);
		return url == null ? null : url.getPath();
	}
}
