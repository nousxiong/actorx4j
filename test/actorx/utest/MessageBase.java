/**
 * 
 */
package actorx.utest;

import static org.junit.Assert.*;

import org.junit.Test;

import actorx.ActorId;
import actorx.Message;
import actorx.util.StringUtils;

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
		msg1.put(dat1);
		msg1.put(str1);
		
		Message msg2 = Message.make(msg1);
		Data dat = msg2.getRaw();
		assertTrue(dat1.equals(dat));
		
		msg2.put(dat2);
		String str = msg2.getString();
		assertTrue(str1.equals(str));
		
		dat = msg2.getRaw();
		assertTrue(dat2.equals(dat));
		
		dat = msg1.getRaw();
		assertTrue(dat1.equals(dat));
		str = msg1.getString();
		assertTrue(str1.equals(str));
	}
	
	@Test
	public void testReset(){
		Data dat1 = new Data(1, 2);
		Data dat2 = new Data(2, 3);
		String str1 = "INIT";
		assertTrue(!dat1.equals(dat2));
		
		Message msg1 = Message.make();
		msg1.put(dat1);
		msg1.put(str1);
		assertTrue(msg1.getRaw() == dat1);
		assertTrue(str1.equals(msg1.getString()));
		
		Message msg2 = Message.make(msg1);
		Data dat = msg2.getRaw();
		assertTrue(dat1.equals(dat));
		
		msg2.put(dat2);
		String str = msg2.getString();
		assertTrue(str1.equals(str));
		
		msg2.resetRead();
		assertTrue(msg2.getRaw() == dat);
		assertTrue(msg2.getString() == str);
		
		dat = msg2.getRaw();
		assertTrue(dat2.equals(dat));

		msg1.resetRead();
		dat = msg1.getRaw();
		assertTrue(dat1.equals(dat));
		str = msg1.getString();
		assertTrue(str1.equals(str));
	}
	
	@Test
	public void testBatch(){
		Message bmsg = Message.make();
		
		Message msg1 = Message.make();
		ActorId actorId1 = new ActorId(0, 0, 1);
		msg1.put(1);
		msg1.put("msg1");
		msg1.put(actorId1);
		bmsg.put(msg1);
		
		Message msg2 = Message.make();
		ActorId actorId2 = new ActorId(0, 0, 2);
		msg2.put(2);
		msg2.put("msg2");
		msg2.put(actorId2);
		bmsg.put(msg2);
		
		Message msg3 = Message.make();
		ActorId actorId3 = new ActorId(0, 0, 3);
		msg3.put(3);
		msg3.put("msg3");
		msg3.put(actorId3);
		bmsg.put(msg3);
		
		Message bmsg_ = Message.make(bmsg);
		Message msg1_ = bmsg_.get(Message.make());
		Message msg2_ = bmsg_.get(Message.make());
		Message msg3_ = bmsg_.get(Message.make());
		
		assertTrue(msg1_.getInt() == msg1.getInt());
		assertTrue(StringUtils.equals(msg1_.getString(), msg1.getString()));
		assertTrue(ActorId.equals(msg1_.get(ActorId.class), msg1.get(ActorId.class)));
		
		assertTrue(msg2_.getInt() == msg2.getInt());
		assertTrue(StringUtils.equals(msg2_.getString(), msg2.getString()));
		assertTrue(ActorId.equals(msg2_.get(ActorId.class), msg2.get(ActorId.class)));
		
		assertTrue(msg3_.getInt() == msg3.getInt());
		assertTrue(StringUtils.equals(msg3_.getString(), msg3.getString()));
		assertTrue(ActorId.equals(msg3_.get(ActorId.class), msg3.get(ActorId.class)));
		
		msg1.release();
		msg2.release();
		msg3.release();
		msg1_.release();
		msg2_.release();
		msg3_.release();
	}

}
