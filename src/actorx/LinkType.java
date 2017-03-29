/**
 * 
 */
package actorx;

/**
 * @author Xiong
 * actor链接类型
 */
public enum LinkType {
	NO_LINK, // 无链接
	LINKED, // 双向链接，两个actor互相监视
	MONITORED, // 单向链接，监视被链接的actor
}
