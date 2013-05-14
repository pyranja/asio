package at.ac.univie.isc.asio.frontend;

import static java.util.Collections.singleton;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class SqlQueryEndpointTest extends EndpointTestFixture {

	private static final byte[] PAYLOAD = "TEST-PAYLOAD"
			.getBytes(Charsets.UTF_8);
	private static final SerializationFormat MOCK_FORMAT = new SerializationFormat() {
		@Override
		public com.google.common.net.MediaType asMediaType() {
			return com.google.common.net.MediaType
					.create("application", "test");
		}
	};
	private static final MediaType MOCK_CONTENT_TYPE = MediaType
			.valueOf("application/test");

	private Response response;

	@Before
	public void setUp() {
		when(engine.supportedFormats()).thenReturn(singleton(MOCK_FORMAT));
		endpoint.initializeVariants();
		client.path("query").accept(MOCK_CONTENT_TYPE);
	}

	@After
	public void tearDown() {
		response.close();
	}

	// HAPPY PATH

	@Test
	public void success_response_has_ok_status() throws Exception {
		when(engine.submit("test-query")).thenReturn(successFuture());
		response = client.query("query", "test-query").get();
		assertEquals(Status.OK, fromStatusCode(response.getStatus()));
	}

	@Test
	public void success_response_has_requested_content_type() throws Exception {
		when(engine.submit("test-query")).thenReturn(successFuture());
		response = client.query("query", "test-query").get();
		assertEquals(MediaType.valueOf("application/test"),
				response.getMediaType());
	}

	@Test
	public void success_response_contains_result_data() throws Exception {
		when(engine.submit("test-query")).thenReturn(successFuture());
		response = client.query("query", "test-query").get();
		final byte[] received = ByteStreams.toByteArray((InputStream) response
				.getEntity());
		assertArrayEquals(PAYLOAD, received);
	}

	// QUERY METHOD VARIATIONS

	@Test
	public void accepts_from_query_string() throws Exception {
		response = client.query("query", "test-query").get();
		verify(engine).submit("test-query");
	}

	@Test
	public void accepts_form_encoded_post() throws Exception {
		final Form values = new Form();
		values.set("query", "test-query");
		response = client.form(values);
		verify(engine).submit("test-query");
	}

	@Test
	public void accepts_plain_query_as_body() throws Exception {
		response = client.type("application/sql-query").post("test-query");
		verify(engine).submit("test-query");
	}

	// REJECTIONS

	@Test
	public void rejects_non_query_content_body() throws Exception {
		client.type(MediaType.TEXT_HTML_TYPE);
		response = client.post("query");
		assertEquals(Status.UNSUPPORTED_MEDIA_TYPE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(anyString());
	}

	@Test
	public void rejects_unsupported_accept_type() throws Exception {
		client.reset().path("query").accept(MediaType.TEXT_HTML_TYPE);
		response = client.get();
		assertEquals(Status.NOT_ACCEPTABLE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(anyString());
	}

	// ERROR PROPAGATION

	@SuppressWarnings("unchecked")
	@Test
	public void usage_error_generates_bad_request_response() throws Exception {
		when(engine.submit(anyString())).thenThrow(DatasetUsageException.class);
		response = client.get();
		assertEquals(Status.BAD_REQUEST, fromStatusCode(response.getStatus()));
	}

	@Test
	public void usage_error_response_contains_cause() throws Exception {
		final DatasetUsageException cause = new DatasetUsageException(
				"test-message");
		when(engine.submit(anyString())).thenThrow(cause);
		response = client.get();
		final String received = new String(
				ByteStreams.toByteArray((InputStream) response.getEntity()));
		assertEquals(cause.getMessage(), received);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void dataset_error_generates_server_error_response()
			throws Exception {
		when(engine.submit(anyString())).thenThrow(DatasetException.class);
		response = client.get();
		assertEquals(Status.INTERNAL_SERVER_ERROR,
				fromStatusCode(response.getStatus()));
	}

	@Test
	public void dataset_error_response_contains_cause() throws Exception {
		final DatasetException cause = new DatasetUsageException("test-message");
		when(engine.submit(anyString())).thenThrow(cause);
		response = client.get();
		final String received = new String(
				ByteStreams.toByteArray((InputStream) response.getEntity()));
		assertEquals(cause.getMessage(), received);
	}

	private ListenableFuture<InputSupplier<InputStream>> successFuture() {
		final SettableFuture<InputSupplier<InputStream>> future = SettableFuture
				.create();
		future.set(new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return new ByteArrayInputStream(PAYLOAD);
			}
		});
		return future;
	}
}
