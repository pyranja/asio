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

import at.ac.univie.isc.asio.Result;

import com.google.common.io.InputSupplier;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ListenableFuture;

public class CompletionResultHandlerTest {

	private static final MediaType MOCK_TYPE = MediaType.create("test", "type");

	private CompletionResultHandler subject;
	private MockBuffer buffer;

	@Before
	public void setUp() {
		buffer = new MockBuffer();
		subject = new CompletionResultHandler(buffer, MOCK_TYPE);
	}

	@Test(expected = NullPointerException.class)
	public void must_give_cause_on_fail() throws Exception {
		subject.fail(null);
	}

	@Test(timeout = 100)
	public void fail_completion_rethrows_wrapped_cause() throws Exception {
		final Exception cause = new IllegalStateException();
		final ListenableFuture<Result> future = subject.asFutureResult();
		subject.fail(cause);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertSame(cause, e.getCause());
		}
	}

	@Test(timeout = 100)
	public void success_completion_returns_buffer_stream() throws Exception {
		final ListenableFuture<Result> future = subject.asFutureResult();
		subject.complete();
		final InputSupplier<InputStream> result = future.get();
		assertSame(buffer.stream, result.getInput());
	}

	@Test(timeout = 100)
	public void success_completion_returns_set_content_type() throws Exception {
		final ListenableFuture<Result> future = subject.asFutureResult();
		subject.complete();
		final Result result = future.get();
		assertSame(MOCK_TYPE, result.mediaType());
	}

	@Test(expected = TimeoutException.class)
	public void provided_future_blocks_if_not_completed() throws Exception {
		final ListenableFuture<Result> future = subject.asFutureResult();
		future.get(10, TimeUnit.MILLISECONDS);
	}

	// mock result buffer
	private final static class MockBuffer implements Buffer {
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(
				4096);

		private final InputStream stream = new ByteArrayInputStream(
				buffer.toByteArray());

		@Override
		public OutputStream getOutput() throws IOException {
			return buffer;
		}

		@Override
		public InputStream getInput() throws IOException {
			return stream;
		}
	}
}
