package at.ac.univie.isc.asio.frontend;

import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.MockFormats;
import at.ac.univie.isc.asio.MockResult;

import com.google.common.io.ByteStreams;

@RunWith(MockitoJUnitRunner.class)
public class UpdateEndpointTest extends EndpointTestFixture {

	private Response response;
	@Captor private ArgumentCaptor<DatasetOperation> submitted;

	@Before
	public void setUp() {
		client.path("update").accept(MockFormats.APPLICABLE_CONTENT_TYPE);
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
		response = client.type("application/sql-update").post("test-update");
		assertEquals(Status.OK, fromStatusCode(response.getStatus()));
	}

	@Test
	public void success_response_has_requested_content_type() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.type("application/sql-update").post("test-update");
		assertEquals(MockFormats.APPLICABLE_CONTENT_TYPE,
				response.getMediaType());
	}

	@Test
	public void success_response_contains_result_data() throws Exception {
		when(engine.submit(any(DatasetOperation.class))).thenReturn(
				MockResult.successFuture());
		response = client.type("application/sql-update").post("test-update");
		final byte[] received = ByteStreams.toByteArray((InputStream) response
				.getEntity());
		assertArrayEquals(MockResult.PAYLOAD, received);
	}

	// INVOCATION PATHS

	@Test
	public void accepts_form_encoded_post() throws Exception {
		final Form values = new Form();
		values.set("update", "test-update");
		response = client.form(values);
		verifyInvocation();
	}

	@Test
	public void accepts_plain_update_as_body() throws Exception {
		response = client.type("application/sql-update").post("test-update");
		verifyInvocation();
	}

	private void verifyInvocation() {
		verify(engine).submit(submitted.capture());
		final DatasetOperation op = submitted.getValue();
		assertEquals(Action.UPDATE, op.action());
		assertEquals("test-update", op.command().orNull());
		assertEquals(VALID_FORMAT, op.format());
	}

	// REJECTIONS

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
		response = client.post("update");
		assertEquals(Status.UNSUPPORTED_MEDIA_TYPE,
				fromStatusCode(response.getStatus()));
		verify(engine, never()).submit(any(DatasetOperation.class));
	}

}
