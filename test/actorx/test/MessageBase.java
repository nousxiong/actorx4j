/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.Message;

/**
 * @author Xiong
 * 测试消息
 */
public class MessageBase {

	static class Data {
		public Data(int i, long index){
			this.id = i;
			this.index = index;
		}
		
		public int getId() {
			return id;
		}
		public long getIndex() {
			return index;
		}
		
		@Override
		public boolean equals(Object o){
			if (this == o){
				return true;
			}
			
			if (o == null || getClass() != o.getClass()){
				return false;
			}
			
			Data rhs = (Data)o;
			return 
				this.getId() == rhs.getId() && 
				this.getIndex() == rhs.getIndex();
		}

		private int id;
		private long index;
	}
	
	@Test
	public void test() {
		Data dat1 = new Data(1, 2);
		Data dat2 = new Data(2, 3);
		String str1 = "INIT";
		assertTrue(!dat1.equals(dat2));
		
		Message msg1 = Message.make();
		msg1.write(dat1);
		msg1.write(str1);
		
		Message msg2 = Message.make(msg1);
		Data dat = msg2.read();
		assertTrue(dat1.equals(dat));
		
		msg2.write(dat2);
		String str = msg2.read();
		assertTrue(str1.equals(str));
		
		dat = msg2.read();
		assertTrue(dat2.equals(dat));
		
		dat = msg1.read();
		assertTrue(dat1.equals(dat));
		str = msg1.read();
		assertTrue(str1.equals(str));
		
		assertTrue(msg1.read() == null);
		assertTrue(msg2.read() == null);
	}

}
