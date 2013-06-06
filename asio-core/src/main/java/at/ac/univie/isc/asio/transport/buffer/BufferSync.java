package at.ac.univie.isc.asio.transport.buffer;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Maintains the state of a persistent buffer.
 * 
 * <p>
 * Attach to the input channel of the buffer to record writes to the buffer.
 * {@link #limit()} will hold the current size of the buffer.
 * </p>
 * 
 * <p>
 * This implementation is safe for multi-threaded use. Note that the buffer's
 * limit <strong>may</strong> change, even after {@link #isGrowing()} returns
 * false, due to delayed updates.
 * </p>
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
class BufferSync {
	/*
	 * Uses a single lock to guard buffer state modifications. Additionally
	 * await() establishes a happen-before relationship, i.e. a thread returning
	 * from await() is guaranteed to see at least the state that caused await to
	 * return. Therefore after returning from await() the buffer's limit will be
	 * greater than or equal to the requiredSize given when calling await() OR
	 * the buffer is not growing anymore.
	 */
	// state
	private long limit;
	private boolean growing;
	// concurrency
	private final ReentrantLock lock;
	private final Queue<SizeChangeLatch> waiting;

	BufferSync() {
		super();
		limit = 0;
		growing = true;
		waiting = new PriorityQueue<>();
		lock = new ReentrantLock();
	}

	public void freeze() {
		try {
			lock.lock();
			growing = false;
			awakeWaiter();
		} finally {
			lock.unlock();
		}
	}

	public void grow(final int written) {
		checkArgument(written >= 0, "negative size on channel write event");
		try {
			lock.lock();
			limit += written;
			awakeWaiter();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Awake waiting readers if current state permits. Call only if holding the
	 * lock.
	 */
	private void awakeWaiter() {
		if (growing) {
			SizeChangeLatch head = waiting.peek();
			while (head != null && head.requiredSize <= limit) {
				head.latch.countDown();
				waiting.remove();
				head = waiting.peek();
			}
		} else { // awake unconditionally if not growing
			SizeChangeLatch head = waiting.poll();
			while (head != null) {
				head.latch.countDown();
				head = waiting.poll();
			}
		}
	}

	/**
	 * Note: A closed channel cannot be reopened.
	 * 
	 * @return whether the observed channel is still open.
	 */
	public boolean isGrowing() {
		try {
			lock.lock();
			return growing;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return number of bytes written to the observed channel so far.
	 */
	public long limit() {
		try {
			lock.lock();
			return limit;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Waits until the buffer's <code>limit</code> is at least <code>size</code>
	 * bytes or the buffer will not continue to grow. Will return immediately if
	 * this buffer is not growing anymore. Threads blocked on this method may be
	 * interrupted.
	 * 
	 * @param required
	 *            required size of the buffer
	 * @return true if the buffer may continue to grow, false else
	 * @throws InterruptedException
	 *             if interrupted
	 */
	public boolean await(final long required) throws InterruptedException {
		checkArgument(required >= 0, "cannot await negative size");
		final SizeChangeLatch waiter = new SizeChangeLatch(required);
		try {
			lock.lockInterruptibly();
			if (!growing || required <= limit) {
				return growing;
			}
			waiting.offer(waiter);
		} finally {
			lock.unlock();
		}
		waiter.latch.await();
		return growing; // synced by latch await
	}

	@Override
	public String toString() {
		return String.format("BufferSync [limit=%s, open=%s]", limit, growing);
	}

	/**
	 * Orderable countdown latch wrapper.
	 * 
	 * @author Chris Borckholder
	 */
	private static class SizeChangeLatch implements Comparable<SizeChangeLatch> {
		private final long requiredSize;
		private final CountDownLatch latch;

		public SizeChangeLatch(final long requiredSize) {
			super();
			this.requiredSize = requiredSize;
			latch = new CountDownLatch(1);
		}

		@Override
		public int compareTo(final SizeChangeLatch o) {
			return Long.compare(requiredSize, o.requiredSize);
		}
	}
}
