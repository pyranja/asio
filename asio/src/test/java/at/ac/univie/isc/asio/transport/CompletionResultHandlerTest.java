package at.ac.univie.isc.asio.transport;

import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

public class CompletionResultHandlerTest {

	private CompletionResultHandler subject;
	private ByteArrayBuffer buffer;

	@Before
	public void setUp() {
		buffer = new ByteArrayBuffer();
		subject = new CompletionResultHandler(buffer);
	}

	@Test(expected = NullPointerException.class)
	public void must_give_cause_on_fail() throws Exception {
		subject.fail(null);
	}

	@Test(timeout = 100)
	public void fail_completion_rethrows_wrapped_cause() throws Exception {
		final Exception cause = new IllegalStateException();
		final ListenableFuture<InputSupplier<InputStream>> future = subject
				.asFutureResult();
		subject.fail(cause);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertSame(cause, e.getCause());
		}
	}

	@Test(timeout = 100)
	public void succes_completion_returns_buffer() throws Exception {
		final ListenableFuture<InputSupplier<InputStream>> future = subject
				.asFutureResult();
		subject.complete();
		final InputSupplier<InputStream> result = future.get();
		assertSame(buffer, result);
	}

	@Test(expected = TimeoutException.class)
	public void provided_future_blocks_if_not_completed() throws Exception {
		final ListenableFuture<InputSupplier<InputStream>> future = subject
				.asFutureResult();
		future.get(10, TimeUnit.MILLISECONDS);
	}

	// mock result buffer
	private final static class ByteArrayBuffer implements Buffer {
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(
				4096);

		@Override
		public OutputStream getOutput() throws IOException {
			return buffer;
		}

		@Override
		public InputStream getInput() throws IOException {
			return new ByteArrayInputStream(buffer.toByteArray());
		}
	}
}
