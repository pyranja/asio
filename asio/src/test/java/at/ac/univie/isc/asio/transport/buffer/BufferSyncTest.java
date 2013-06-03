package at.ac.univie.isc.asio.transport.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BufferSyncTest {

	private static final long TIMEOUT = 1;
	private static final TimeUnit UNIT = TimeUnit.SECONDS;

	private BufferSync subject;
	private ExecutorService exec;

	@Before
	public void setUp() {
		subject = new BufferSync();
		exec = Executors.newCachedThreadPool();
	}

	@After
	public void tearDown() {
		exec.shutdownNow();
	}

	@Test
	public void initial_state() {
		assertTrue(subject.isGrowing());
		assertEquals(0, subject.limit());
	}

	@Test
	public void close_propagates() {
		subject.freeze();
		assertFalse(subject.isGrowing());
	}

	@Test
	public void write_events_increase_limit() {
		subject.grow(1);
		assertEquals(1, subject.limit());
		subject.grow(2);
		assertEquals(3, subject.limit());
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannot_await_negative_size() throws Exception {
		subject.await(-1);
	}

	@Test(timeout = 100)
	public void await_returns_true_on_open_buffer() throws Exception {
		subject.grow(10);
		assertTrue(subject.await(5));
	}

	@Test(timeout = 100)
	public void await_returns_false_on_closed_buffer()
			throws InterruptedException {
		subject.grow(10);
		subject.freeze();
		assertFalse(subject.await(5));
	}

	@Test(timeout = 100)
	public void await_returns_false_immediately_if_channel_closed_but_size_not_reached()
			throws Exception {
		subject.freeze();
		assertFalse(subject.await(5));
	}

	@Test(timeout = 100)
	public void await_returns_immediately_if_size_already_reached()
			throws Exception {
		final BufferSync shared = subject;
		shared.grow(10);
		final Future<Boolean> task = exec.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return shared.await(5);
			}
		});
		assertTrue("buffer not indicating buffer continue",
				task.get(TIMEOUT, UNIT));
	}

	@Test(timeout = 100)
	public void await_blocks_until_writes_reach_required_size()
			throws Exception {
		final BufferSync shared = subject; // closure
		final AtomicBoolean blocking = new AtomicBoolean(true);
		final CountDownLatch waiter_started = new CountDownLatch(1);
		final Future<Boolean> task = exec.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				waiter_started.countDown();
				final boolean ret_val = shared.await(10);
				blocking.set(false);
				return ret_val;
			}
		});
		assertTrue("waiter callable not started",
				waiter_started.await(TIMEOUT, UNIT));
		assertTrue("waiter not blocking", blocking.get());
		shared.grow(5);
		Thread.yield();
		assertTrue("waiter unblocked after partial limit raise", blocking.get());
		shared.grow(5);
		assertTrue("await not indicating buffer continue",
				task.get(TIMEOUT, UNIT));
		assertFalse("waiter not unblocked", blocking.get());
	}

	@Test(timeout = 100)
	public void waiter_are_awoken_in_order_of_awaited_size() throws Exception {
		final BufferSync shared = subject;
		final AtomicBoolean small_blocking = new AtomicBoolean(true);
		final AtomicBoolean big_blocking = new AtomicBoolean(true);
		final CountDownLatch small_started = new CountDownLatch(1);
		final CountDownLatch big_started = new CountDownLatch(1);
		final Future<Boolean> small_waiter = exec
				.submit(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						small_started.countDown();
						final boolean ret_val = shared.await(5);
						small_blocking.set(false);
						return ret_val;
					}
				});
		small_started.countDown();
		final Future<Boolean> big_waiter = exec.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				big_started.countDown();
				final boolean ret_val = shared.await(10);
				big_blocking.set(false);
				return ret_val;
			}
		});
		big_started.countDown();
		assertTrue("small not blocking", small_blocking.get());
		assertTrue("big not blocking", big_blocking.get());
		shared.grow(7);
		assertTrue("small waiter not awoken", small_waiter.get());
		assertTrue("big awoken too soon", big_blocking.get());
		shared.grow(3);
		assertTrue("big waiter not awoken", big_waiter.get());
	}

	@Test(timeout = 100)
	public void await_returns_from_blocking_when_buffer_closes()
			throws Exception {
		final BufferSync shared = subject; // closure
		final AtomicBoolean blocking = new AtomicBoolean(true);
		final CountDownLatch waiter_started = new CountDownLatch(1);
		final Future<Boolean> task = exec.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				waiter_started.countDown();
				final boolean ret_val = shared.await(10);
				blocking.set(false);
				return ret_val;
			}
		});
		assertTrue("waiter callable not started",
				waiter_started.await(TIMEOUT, UNIT));
		assertTrue("waiter not blocking", blocking.get());
		shared.freeze();
		assertFalse("await not indicating buffer close",
				task.get(TIMEOUT, UNIT));
		assertFalse("waiter not unblocked", blocking.get());
	}
}
