package at.ac.univie.isc.asio.frontend;

import static at.ac.univie.isc.asio.DatasetOperation.SerializationFormat.NONE;
import static at.ac.univie.isc.asio.MockFormat.ALWAYS_APPLICABLE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.MockOperations;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.ResultRepository;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.util.concurrent.ListenableFuture;

@RunWith(MockitoJUnitRunner.class)
public class EngineAdapterTest {

	private EngineAdapter subject;
	private DatasetOperation op;
	@Mock private DatasetEngine engine;
	@Mock private ResultRepository results;
	@Mock private FormatSelector selector;
	@Mock private Request request;

	@Before
	public void setUp() {
		subject = new EngineAdapter(engine, results, selector);
		op = MockOperations.schema(MockFormat.ALWAYS_APPLICABLE);
	}

	@Test
	public void completes_op_builder_with_selected_format() throws Exception {
		final OperationBuilder partial = new OperationBuilder("test-id",
				Action.BATCH, "test-command");
		when(selector.selectFormat(same(request), any(Action.class)))
				.thenReturn(ALWAYS_APPLICABLE);
		final DatasetOperation created = subject.completeWithMatchingFormat(
				request, partial);
		assertThat(created.format(), is(ALWAYS_APPLICABLE));
		assertThat(created.id(), is("test-id"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sets_context_on_selection_error() throws Exception {
		final OperationBuilder partial = new OperationBuilder("test-id",
				Action.BATCH, "test-command");
		when(selector.selectFormat(same(request), any(Action.class)))
				.thenThrow(DatasetUsageException.class);
		try {
			subject.completeWithMatchingFormat(request, partial);
			fail("usage exception not rethrown");
		} catch (final DatasetUsageException e) {
			assertThat(e.failedOperation().isPresent(), is(true));
			op = e.failedOperation().get();
			assertThat(op.format(), is(NONE));
			assertThat(op.id(), is("test-id"));
		}
	}

	@Test
	public void submits_op_to_engine() throws Exception {
		subject.submit(op);
		verify(engine).submit(same(op), any(ResultHandler.class),
				any(Principal.class));
	}

	@Test
	public void creates_handler_for_op() throws Exception {
		subject.submit(op);
		verify(results).newHandlerFor(op);
	}

	@Test(timeout = 100)
	public void traps_and_wraps_submission_errors_in_result_future()
			throws Exception {
		final DatasetOperation op = MockOperations.query("test",
				ALWAYS_APPLICABLE);
		final Exception error = new IllegalStateException("test");
		doThrow(error).when(engine).submit(any(DatasetOperation.class),
				any(ResultHandler.class), any(Principal.class));
		final ListenableFuture<Result> future = subject.submit(op);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertTrue(e.getCause() instanceof DatasetException);
			assertSame(error, e.getCause().getCause());
		}
	}

	@Test(timeout = 100)
	public void sets_trapped_dataset_error_as_direct_cause() throws Exception {
		final DatasetOperation op = MockOperations.query("test",
				ALWAYS_APPLICABLE);
		final DatasetException error = new DatasetUsageException("test");
		doThrow(error).when(engine).submit(any(DatasetOperation.class),
				any(ResultHandler.class), any(Principal.class));
		final ListenableFuture<Result> future = subject.submit(op);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertTrue(e.getCause() instanceof DatasetException);
			assertSame(error, e.getCause());
		}
	}
}
