package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.MockDatasetException;
import at.ac.univie.isc.asio.MockOperations;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.transport.FileResultRepository;

import com.google.common.io.OutputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiEngineTest {

	private static final SerializationFormat MOCK_FORMAT = OgsadaiFormats.XML;
	private static final String MOCK_QUERY = "test-query";
	private static final String MOCK_ID = MockOperations.TEST_ID;

	private OgsadaiEngine subject;
	@Mock private OgsadaiAdapter ogsadai;
	@Mock private FileResultRepository results;
	@Mock private ResultHandler handler;
	@Mock private WorkflowComposer composer;
	@Mock private Workflow dummyWorkflow;
	@Mock private DaiExceptionTranslator translator;
	private DatasetOperation operation;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws IOException {
		operation = MockOperations.query(MOCK_QUERY, MOCK_FORMAT);
		when(ogsadai.register(eq(MOCK_ID), any(OutputSupplier.class)))
				.thenReturn(MOCK_ID);
		when(results.newHandler(any(SerializationFormat.class))).thenReturn(
				handler);
		when(composer.createFrom(any(DatasetOperation.class))).thenReturn(
				dummyWorkflow);
		subject = new OgsadaiEngine(ogsadai, results, composer, translator);
	}

	// invariances

	@Test
	public void supports_ogsadai_formats() throws Exception {
		final Set<SerializationFormat> supported = subject.supportedFormats();
		final List<OgsadaiFormats> ogsadaiFormats = Arrays
				.asList(OgsadaiFormats.values());
		assertTrue(supported.containsAll(ogsadaiFormats));
		assertTrue(ogsadaiFormats.containsAll(supported));
	}

	// behavior

	@Test
	public void passes_operation_to_composer() throws Exception {
		subject.submit(operation);
		verify(composer).createFrom(same(operation));
	}

	@Test
	public void creates_handler_with_given_format() throws Exception {
		subject.submit(operation);
		verify(results).newHandler(MOCK_FORMAT);
	}

	@Test
	public void registers_handler_with_ogsadai() throws Exception {
		subject.submit(operation);
		verify(ogsadai).register(anyString(), same(handler));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void registers_handler_with_operation_id() throws Exception {
		subject.submit(operation);
		verify(ogsadai).register(eq(MOCK_ID), any(OutputSupplier.class));
	}

	@Test
	public void returns_handler_future() throws Exception {
		@SuppressWarnings("unchecked")
		final ListenableFuture<Result> mockFuture = mock(ListenableFuture.class);
		when(handler.asFutureResult()).thenReturn(mockFuture);
		assertSame(mockFuture, subject.submit(operation));
	}

	@Test
	public void invokes_ogsadai_with_composed_workflow() throws Exception {
		subject.submit(operation);
		verify(ogsadai).invoke(anyString(), same(dummyWorkflow),
				any(CompletionCallback.class));
	}

	// error handling

	@Test
	@Ignore
	// FIXME when stream set directly on activity
	public void revokes_handler_on_request_invoke_exception() throws Exception {
		doThrow(new MockDatasetException()).when(ogsadai).invoke(anyString(),
				any(Workflow.class), any(CompletionCallback.class));
		subject.submit(operation);
		verify(ogsadai).revokeSupplier(MOCK_ID);
	}
}
