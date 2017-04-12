/**
 * 
 */
package actorx.utest;

import org.junit.Test;

import actorx.util.Atom;

/**
 * @author Xiong
 * 测试Atom
 */
public class AtomBase {

	@Test
	public void test() {
		String str1 = "zzzzzzzzzz";
		String str2 = "INIT";
		String str3 = "0";
		long a1 = Atom.to(str1);
		long a2 = Atom.to(str2);
		long a3 = Atom.to(str3);
		
		System.out.println("[" + str1 + "] -> "+a1);
		System.out.println("[" + Atom.from(a1) + "] <- "+ a1);
		System.out.println("[" + str2 + "] -> "+a2);
		System.out.println("[" + Atom.from(a2) + "] <- "+ a2);
		System.out.println("[" + str3 + "] -> "+a3);
		System.out.println("[" + Atom.from(a3) + "] <- "+ a3);
	}

}
