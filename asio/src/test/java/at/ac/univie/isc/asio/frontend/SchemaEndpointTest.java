package at.ac.univie.isc.asio.frontend;

import static java.util.Collections.singleton;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.MockResult;

import com.google.common.io.ByteStreams;

@RunWith(MockitoJUnitRunner.class)
public class SchemaEndpointTest extends EndpointTestFixture {

	private static final SerializationFormat MOCK_FORMAT = new MockFormat();
	private SchemaEndpoint endpoint;
	private Response response;
	@Captor private ArgumentCaptor<DatasetOperation> submittedOperation;

	@Before
	public void setUp() {
		endpoint = application.getSchemaEndpoint();
		when(engine.supportedFormats()).thenReturn(singleton(MOCK_FORMAT));
		endpoint.initializeVariants();
		client.path("schema").accept(MockFormat.MOCK_CONTENT_TYPE);
	}

	@After
	public void tearDown() {
		response.close();
	}

	// HAPPY PATH

	@Test
	public void success_response_has_ok_status() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.get();
		assertEquals(Status.OK, fromStatusCode(response.getStatus()));
	}

	@Test
	public void success_response_has_requested_content_type() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.get();
		assertEquals(MockFormat.MOCK_CONTENT_TYPE, response.getMediaType());
	}

	@Test
	public void success_response_contains_result_data() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.get();
		final byte[] received = ByteStreams.toByteArray((InputStream) response
				.getEntity());
		assertArrayEquals(MockResult.PAYLOAD, received);
	}

	@Test
	public void submits_correct_operation() throws Exception {
		response = client.get();
		Mockito.verify(engine).submit(submittedOperation.capture());
		final DatasetOperation op = submittedOperation.getValue();
		assertEquals(Action.SCHEMA, op.action());
		assertFalse(op.command().isPresent());
		assertEquals(MOCK_FORMAT, op.format());
	}

	// REJECTIONS

	@Test
	public void reject_unsupported_accept_header() throws Exception {
		response = client.reset().path("schema")
				.accept(MediaType.TEXT_HTML_TYPE).get();
		assertEquals(Status.NOT_ACCEPTABLE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}
}
