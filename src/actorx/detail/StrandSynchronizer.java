/**
 * 
 */
package actorx.detail;

import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import cque.util.ISynchronizer;
import cque.util.UnsafeUtils;

/**
 * @author Xiong
 *
 */
public class StrandSynchronizer implements ISynchronizer {
	private volatile Strand waiter;

	@Override
	@Suspendable
	public void await() throws InterruptedException {
		try{
			Strand.park(this);
		}catch (SuspendExecution e){
			throw new AssertionError();
		}

		if (Strand.interrupted()){
			throw new InterruptedException();
		}
	}

	@Override
	@Suspendable
	public void await(long timeout, TimeUnit unit) throws InterruptedException {
		awaitNanos(TimeUnit.NANOSECONDS.convert(timeout, unit));
	}

	@Override
	@Suspendable
	public long awaitNanos(long nanos) throws InterruptedException {
		long left = nanos;
		long deadline = System.nanoTime() + left;
		try {
			Strand.parkNanos(this, left);
		} catch (SuspendExecution e) {
			throw new AssertionError();
		}
		
		if (Strand.interrupted()){
			throw new InterruptedException();
		}
		
		left = deadline - System.nanoTime();
		return left;
	}

	@Override
	public void register() {
		final Strand currentStrand = Strand.currentStrand();
		if (!casWaiter(null, currentStrand)){
			throw new IllegalMonitorStateException("attempt by " + currentStrand + " but owned by " + waiter);
		}
	}

	@Override
	public boolean shouldSignal() {
		return waiter != null;
	}

	@Override
	public void signal() {
		final Strand t = waiter;
		if (t != null){
			Strand.unpark(t);
		}
	}

	@Override
	public void unregister() {
		if (waiter != Strand.currentStrand()){
			throw new IllegalMonitorStateException("attempt by " + Strand.currentStrand() + " but owned by " + waiter);
		}
		waiter = null;
	}

	private static final sun.misc.Unsafe UNSAFE;
	private static final long waiterOffset;

	static {
		try {
			UNSAFE = UnsafeUtils.getUnsafe();
			waiterOffset = UNSAFE.objectFieldOffset(StrandSynchronizer.class.getDeclaredField("waiter"));
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	private boolean casWaiter(Strand expected, Strand update) {
		return UNSAFE.compareAndSwapObject(this, waiterOffset, expected, update);
	}

}
