package at.ac.univie.isc.asio.frontend;

import static at.ac.univie.isc.asio.MockFormat.ALWAYS_APPLICABLE;
import static at.ac.univie.isc.asio.MockFormat.NEVER_APPLICABLE;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.MockOperations;
import at.ac.univie.isc.asio.Result;

import com.google.common.util.concurrent.ListenableFuture;

@RunWith(MockitoJUnitRunner.class)
public class FrontentEngineAdapterTest {

	private FrontendEngineAdapter subject;
	@Mock private DatasetEngine engine;
	@Mock private Request request;
	private final VariantConverter converter = new VariantConverter();

	@Before
	public void setUp() {
		when(engine.supportedFormats())
				.thenReturn(singleton(ALWAYS_APPLICABLE));
		subject = new FrontendEngineAdapter(engine, converter);
	}

	@Test
	public void forwards_engines_supported_formats() throws Exception {
		final Set<SerializationFormat> expected = singleton(ALWAYS_APPLICABLE);
		assertEquals(expected, subject.supportedFormats());
	}

	@Test
	public void forwards_operation_to_engine() throws Exception {
		final DatasetOperation op = MockOperations.query("test",
				ALWAYS_APPLICABLE);
		subject.submit(op);
		verify(engine).submit(op);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void passes_applicable_format_variants_to_request_selector()
			throws Exception {
		when(request.selectVariant(anyList())).thenReturn(
				new Variant(null, "en", null));
		subject.selectFormat(request, Action.QUERY);
		verify(request).selectVariant(anyList());
	}

	@Test(expected = WebApplicationException.class)
	public void fails_if_no_format_applicable() throws Exception {
		when(engine.supportedFormats()).thenReturn(singleton(NEVER_APPLICABLE));
		subject.selectFormat(request, Action.QUERY);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = WebApplicationException.class)
	public void fails_if_request_selector_returns_null() throws Exception {
		when(request.selectVariant(anyList())).thenReturn(null);
		subject.selectFormat(request, Action.QUERY);
	}

	@Test(timeout = 100)
	public void traps_and_wraps_submission_errors_in_result_future()
			throws Exception {
		final DatasetOperation op = MockOperations.query("test",
				ALWAYS_APPLICABLE);
		final Exception error = new IllegalStateException("test");
		when(engine.submit(any(DatasetOperation.class))).thenThrow(error);
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
		when(engine.submit(any(DatasetOperation.class))).thenThrow(error);
		final ListenableFuture<Result> future = subject.submit(op);
		try {
			future.get();
		} catch (final ExecutionException e) {
			assertTrue(e.getCause() instanceof DatasetException);
			assertSame(error, e.getCause());
		}
	}
}
