package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.test.MockDatasetException;
import at.ac.univie.isc.asio.transport.FileResultRepository;

import com.google.common.io.OutputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiEngineTest {

	private static final ResourceID RESOURCE = new ResourceID("test");

	private static final SerializationFormat MOCK_FORMAT = OgsadaiFormats.XML;
	private static final String MOCK_QUERY = "test-query";
	private static final String MOCK_STREAM_ID = "test-stream";

	private OgsadaiEngine subject;
	@Mock private OgsadaiAdapter ogsadai;
	@Mock private FileResultRepository results;
	@Mock private ResultHandler handler;
	private DatasetOperation operation;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws IOException {
		operation = DatasetOperation.query(MOCK_QUERY, MOCK_FORMAT);
		when(ogsadai.register(any(OutputSupplier.class))).thenReturn(
				MOCK_STREAM_ID);
		when(results.newHandler(any(SerializationFormat.class))).thenReturn(
				handler);
		subject = new OgsadaiEngine(ogsadai, results, RESOURCE);
	}

	// invariances

	@Test(expected = DatasetUsageException.class)
	public void reject_null_query() throws Exception {
		final DatasetOperation op = DatasetOperation.query(null, MOCK_FORMAT);
		subject.submit(op);
	}

	@Test(expected = DatasetUsageException.class)
	public void reject_empty_query() throws Exception {
		final DatasetOperation op = DatasetOperation.query("", MOCK_FORMAT);
		subject.submit(op);
	}

	@Test
	public void supports_ogsadai_formats() throws Exception {
		final Set<SerializationFormat> supported = subject.supportedFormats();
		final List<OgsadaiFormats> ogsadaiFormats = Arrays
				.asList(OgsadaiFormats.values());
		assertTrue(supported.containsAll(ogsadaiFormats)); // XXX only for QUERY
		assertTrue(ogsadaiFormats.containsAll(supported));
	}

	// behavior

	@Test
	public void creates_handler_with_given_format() throws Exception {
		subject.submit(operation);
		verify(results).newHandler(MOCK_FORMAT);
	}

	@Test
	public void registers_handler_with_ogsadai() throws Exception {
		subject.submit(operation);
		verify(ogsadai).register(handler);
	}

	@Test
	public void returns_handler_future() throws Exception {
		@SuppressWarnings("unchecked")
		final ListenableFuture<Result> mockFuture = mock(ListenableFuture.class);
		when(handler.asFutureResult()).thenReturn(mockFuture);
		assertSame(mockFuture, subject.submit(operation));
	}

	// error handling

	@Test
	public void revokes_handler_on_request_invoke_exception() throws Exception {
		when(ogsadai.invoke(any(Workflow.class), any(CompletionCallback.class)))
				.thenThrow(new MockDatasetException());
		subject.submit(operation);
		verify(ogsadai).revokeSupplier(MOCK_STREAM_ID);
	}

	@Test
	public void handler_fail_called_on_request_invoke_exception()
			throws Exception {
		final DatasetException cause = new MockDatasetException();
		when(ogsadai.invoke(any(Workflow.class), any(CompletionCallback.class)))
				.thenThrow(cause);
		subject.submit(operation);
		verify(handler).fail(cause);
	}
}
