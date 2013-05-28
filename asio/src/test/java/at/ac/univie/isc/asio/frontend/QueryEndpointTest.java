package at.ac.univie.isc.asio.frontend;

import static at.ac.univie.isc.asio.MockFormat.APPLICABLE_CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.MockResult;

import com.google.common.io.ByteStreams;

@RunWith(MockitoJUnitRunner.class)
public class QueryEndpointTest extends EndpointTestFixture {

	private Response response;
	@Captor private ArgumentCaptor<DatasetOperation> submittedOperation;

	@Before
	public void setUp() {
		client.path("query").accept(APPLICABLE_CONTENT_TYPE);
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
		response = client.query("query", "test-query").get();
		assertEquals(Status.OK, fromStatusCode(response.getStatus()));
	}

	@Test
	public void success_response_has_requested_content_type() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.query("query", "test-query").get();
		assertEquals(MockFormat.APPLICABLE_CONTENT_TYPE,
				response.getMediaType());
	}

	@Test
	public void success_response_contains_result_data() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.query("query", "test-query").get();
		final byte[] received = ByteStreams.toByteArray((InputStream) response
				.getEntity());
		assertArrayEquals(MockResult.PAYLOAD, received);
	}

	// QUERY METHOD VARIATIONS

	@Test
	public void accepts_from_query_string() throws Exception {
		response = client.query("query", "test-query").get();
		verifyInvocation();
	}

	@Test
	public void accepts_form_encoded_post() throws Exception {
		final Form values = new Form();
		values.set("query", "test-query");
		response = client.form(values);
		verifyInvocation();
	}

	@Test
	public void accepts_plain_query_as_body() throws Exception {
		response = client.type("application/sql-query").post("test-query");
		verifyInvocation();
	}

	private void verifyInvocation() {
		verify(engine).submit(submittedOperation.capture());
		final DatasetOperation op = submittedOperation.getValue();
		assertEquals(Action.QUERY, op.action());
		assertEquals("test-query", op.command().orNull());
		assertEquals(MockFormat.ALWAYS_APPLICABLE, op.format());
	}

	// REJECTIONS

	@Test
	public void rejects_missing_query_param() throws Exception {
		response = client.get();
		assertEquals(Status.BAD_REQUEST, fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}

	@Test
	public void rejects_missing_form_param() throws Exception {
		final Form values = new Form();
		values.set("invalid", "anything");
		response = client.form(values);
		assertEquals(Status.BAD_REQUEST, fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}

	@Test
	public void rejects_non_query_content_body() throws Exception {
		client.type(MediaType.TEXT_HTML_TYPE);
		response = client.post("query");
		assertEquals(Status.UNSUPPORTED_MEDIA_TYPE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}

	@Test
	public void rejects_unsupported_accept_type() throws Exception {
		client.reset().path("query").query("query", "test-query")
				.accept(MediaType.TEXT_HTML_TYPE);
		response = client.get();
		assertEquals(Status.NOT_ACCEPTABLE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}

	// ERROR PROPAGATION

	@SuppressWarnings("unchecked")
	@Test
	public void usage_error_generates_bad_request_response() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenThrow(
				DatasetUsageException.class);
		response = client.query("query", "test-query").get();
		assertEquals(Status.BAD_REQUEST, fromStatusCode(response.getStatus()));
	}

	@Test
	public void usage_error_response_contains_cause() throws Exception {
		final DatasetUsageException cause = new DatasetUsageException(
				"test-message");
		when(engine.submit(any(DatasetOperation.class))).thenThrow(cause);
		response = client.query("query", "test-query").get();
		final String received = new String(
				ByteStreams.toByteArray((InputStream) response.getEntity()));
		assertThat(received, startsWith("[ERROR] " + cause.getMessage()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void dataset_error_generates_server_error_response()
			throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenThrow(
				DatasetException.class);
		response = client.query("query", "test-query").get();
		assertEquals(Status.INTERNAL_SERVER_ERROR,
				fromStatusCode(response.getStatus()));
	}

	@Test
	public void dataset_error_response_contains_cause() throws Exception {
		final DatasetException cause = new DatasetUsageException("test-message");
		when(engine.submit(any(DatasetOperation.class))).thenThrow(cause);
		response = client.query("query", "test-query").get();
		final String received = new String(
				ByteStreams.toByteArray((InputStream) response.getEntity()));
		assertThat(received, startsWith("[ERROR] " + cause.getMessage()));
	}
}
