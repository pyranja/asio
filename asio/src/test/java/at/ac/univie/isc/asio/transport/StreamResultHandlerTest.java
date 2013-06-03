package at.ac.univie.isc.asio.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.theInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.MockDatasetException;
import at.ac.univie.isc.asio.Result;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(MockitoJUnitRunner.class)
public class StreamResultHandlerTest {

	private static final byte[] PAYLOAD = "TEST!1234".getBytes(Charsets.UTF_8);

	private StreamResultHandler subject;
	@Mock private OutputStream buffer;
	@Mock private Result result;

	private ListenableFuture<Result> future;

	@Before
	public void setUp() throws IOException {
		subject = new StreamResultHandler(buffer, result);
	}

	@Test
	public void provided_stream_writes_to_set_buffer() throws Exception {
		final OutputStream stream = subject.getOutput();
		stream.write(PAYLOAD);
		verify(buffer).write(PAYLOAD);
	}

	@Test
	public void provided_stream_propagates_closing() throws Exception {
		final OutputStream stream = subject.getOutput();
		stream.close();
		verify(buffer).close();
	}

	@Test(expected = TimeoutException.class)
	public void future_blocks_if_no_action() throws Exception {
		final ListenableFuture<Result> future = subject.asFutureResult();
		future.get(10, TimeUnit.MILLISECONDS);
	}

	@Test(timeout = 100)
	public void returns_set_future_on_completion() throws Exception {
		future = subject.asFutureResult();
		subject.complete();
		assertThat(future.get(), sameInstance(result));
	}

	@Test(timeout = 100)
	public void rethrows_cause_on_failure() throws Exception {
		final DatasetException cause = new MockDatasetException();
		future = subject.asFutureResult();
		subject.fail(cause);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertThat((DatasetException) e.getCause(), is(theInstance(cause)));
		}
	}

	@Test(timeout = 100)
	public void completes_on_first_write_to_buffer() throws Exception {
		future = subject.asFutureResult();
		subject.getOutput();
		assertThat(future.get(), is(theInstance(result)));
	}
}
