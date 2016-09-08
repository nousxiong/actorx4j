/**
 * 
 */
package actorx.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import actorx.ActorId;
import actorx.Atom;
import actorx.Message;
import actorx.MsgType;
import actorx.util.IMail;
import actorx.util.Mailbox;
import actorx.util.TypeComparator;

/**
 * @author Xiong
 * 测试邮件箱
 */
public class MailboxBase {
	private static Mailbox mailbox;
	private static ActorId[] aids;
	private static String[] types;
	private static Message[] msgs;
	private static final int AID_CNT = 1000;
	private static final int MSG_CNT = 1000;
	private static final int TYPE_CNT = 10;

	private static Message makeMessage(ActorId sender, String type){
		Message msg = new Message();
		msg.setSender(sender);
		msg.setType(type);
		return msg;
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		long timestamp = System.currentTimeMillis();
		Random r = new Random(timestamp);

		mailbox = new Mailbox();
		aids = new ActorId[AID_CNT];
		for (int i=0; i<aids.length; ++i){
			aids[i] = new ActorId(Atom.to("AXS"), timestamp, i, i);
		}
		types = new String[TYPE_CNT];
		for (int i=0; i<types.length; ++i){
			types[i] = "TYPE" + i;
		}
		
		msgs = new Message[MSG_CNT];
		for (int i=0; i<msgs.length; ++i){
			Message msg = Message.make();
			msg.setSender(aids[r.nextInt(aids.length)]);
			msg.setType(types[r.nextInt(types.length)]);
			msgs[i] = msg;
		}
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		for (Message msg : msgs){
			msg.release();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		clearMailbox();
	}
	
	private static int clearMailbox(){
		int i = 0;
		while (mailbox.fetch(null, null) != null){
			++i;
		}
		return i;
	}

	@Test
	public void testTotal() {
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		for (int i=0; i<msgNum; ++i){
			assertTrue(mailbox.fetch(null, null) == msgs[i]);
		}
	}

	@Test
	public void testType() {
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<String> matchedTypes = new ArrayList<String>(1);
		matchedTypes.add(msgs[1].getType());
		
		IMail mail = mailbox.fetch(matchedTypes, null);
		assertTrue(mail != null);
		assertTrue(TypeComparator.compare(mail.getType(), msgs[1].getType()) == 0);
		
		assertTrue(clearMailbox() == msgNum - 1);
	}

	@Test
	public void testType2() {
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<String> matchedTypes = new ArrayList<String>(1);
		matchedTypes.add(MsgType.NULLTYPE);
		
		IMail mail = mailbox.fetch(matchedTypes, null);
		assertTrue(mail == null);
		
		assertTrue(clearMailbox() == msgNum);
	}

	@Test
	public void testType3() {
		final int msgNum = 100;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<String> matchedTypes = new ArrayList<String>(2);
		matchedTypes.add(msgs[1].getType());
		matchedTypes.add(msgs[7].getType());
		
		for (int i=0; i<2; ++i){
			IMail mail = mailbox.fetch(matchedTypes, null);
			assertTrue(mail != null);
			assertTrue(
				TypeComparator.compare(mail.getType(), msgs[1].getType()) == 0 ||
				TypeComparator.compare(mail.getType(), msgs[7].getType()) == 0
			);
		}
		
		assertTrue(clearMailbox() == msgNum - 2);
	}
	
	@Test
	public void testSender(){
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<Object> matchedActors = new ArrayList<Object>(1);
		matchedActors.add(msgs[1].getSender());
		
		IMail mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(ActorId.compare(mail.getSender(), msgs[1].getSender()) == 0);
		
		assertTrue(clearMailbox() == msgNum - 1);
	}
	
	@Test
	public void testSender2(){
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<Object> matchedActors = new ArrayList<Object>(1);
		matchedActors.add(ActorId.NULLAID);
		
		IMail mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail == null);
		
		assertTrue(clearMailbox() == msgNum);
	}

	@Test
	public void testSender3() {
		final int msgNum = 100;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<Object> matchedActors = new ArrayList<Object>(2);
		matchedActors.add(msgs[1].getSender());
		matchedActors.add(msgs[7].getSender());
		
		for (int i=0; i<2; ++i){
			IMail mail = mailbox.fetch(null, matchedActors);
			assertTrue(mail != null);
			assertTrue(
				ActorId.compare(mail.getSender(), msgs[1].getSender()) == 0 ||
				ActorId.compare(mail.getSender(), msgs[7].getSender()) == 0
			);
		}
		
		assertTrue(clearMailbox() == msgNum - 2);
	}

	@Test
	public void testSenderType() {
		final int msgNum = 10;
		for (int i=0; i<msgNum; ++i){
			mailbox.add(msgs[i]);
		}
		
		List<Object> matchedActors = new ArrayList<Object>(2);
		matchedActors.add(msgs[1].getSender());
		matchedActors.add(msgs[1].getType());
		
		IMail mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(ActorId.compare(mail.getSender(), msgs[1].getSender()) == 0);
		assertTrue(TypeComparator.compare(mail.getType(), msgs[1].getType()) == 0);
		
		assertTrue(clearMailbox() == msgNum - 1);
	}
	
	@Test
	public void testTypeAndSender(){
		Message[] msgs = new Message[5];
		msgs[0] = makeMessage(aids[1], types[1]);
		msgs[1] = makeMessage(aids[2], types[1]);
		msgs[2] = makeMessage(aids[2], types[1]);
		msgs[3] = makeMessage(aids[1], types[2]);
		msgs[4] = makeMessage(aids[1], types[3]);
		
		for (Message msg : msgs){
			mailbox.add(msg);
		}
		
		List<String> matchedTypes = new ArrayList<String>();
		matchedTypes.add(types[1]);
		List<Object> matchedActors = new ArrayList<Object>();
		matchedActors.add(aids[2]);
		matchedActors.add(types[1]);
		matchedActors.add(aids[1]);
		
		IMail mail = null;
		
		mail = mailbox.fetch(matchedTypes, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[0]);
		
		mail = mailbox.fetch(matchedTypes, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[1]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[2]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[3]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[4]);
	}
	
	@Test
	public void testTypeAndSender2(){
		Message[] msgs = new Message[8];
		msgs[0] = makeMessage(aids[1], types[1]);
		msgs[1] = makeMessage(aids[2], types[1]);
		msgs[2] = makeMessage(aids[2], types[1]);
		msgs[3] = makeMessage(aids[1], types[2]);
		msgs[4] = makeMessage(aids[1], types[3]);
		
		msgs[5] = makeMessage(aids[3], types[3]);
		msgs[6] = makeMessage(aids[3], types[4]);
		msgs[7] = makeMessage(aids[3], types[5]);
		
		for (Message msg : msgs){
			mailbox.add(msg);
		}
		
		List<String> matchedTypes = new ArrayList<String>();
		matchedTypes.add(types[1]);
		matchedTypes.add(types[3]);
		List<Object> matchedActors = new ArrayList<Object>();
		matchedActors.add(aids[3]);
		matchedActors.add(types[3]);
		matchedActors.add(types[4]);
		matchedActors.add(aids[1]);
		
		IMail mail = null;
		
		mail = mailbox.fetch(matchedTypes, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[0]);
		
		mail = mailbox.fetch(matchedTypes, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[1]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[5]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[6]);
		
		mail = mailbox.fetch(null, matchedActors);
		assertTrue(mail != null);
		assertTrue(mail == msgs[3]);
	}
}
