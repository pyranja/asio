package at.ac.univie.isc.asio;

import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class SqlQueryTest {

	private static final String PARAM_QUERY = "query";
	private static final String VALID_QUERY = "SELECT COUNT(*) FROM \"person\"";

	private WebClient client;

	@Before
	public void setUp() {
		client = WebClient.create("http://localhost:8080/v1/asio/query");
		client.accept(APPLICATION_XML_TYPE);
	}

	@After
	public void tearDown() {
		client.reset();
	}

	@Test
	public void valid_query_as_uri_param() throws Exception {
		client.query(PARAM_QUERY, VALID_QUERY);
		final Response response = client.get();
		verify(response);
	}

	@Test
	public void valid_query_as_form_param() throws Exception {
		final Form values = new Form();
		values.set(PARAM_QUERY, VALID_QUERY);
		final Response response = client.form(values);
		verify(response);
	}

	@Test
	public void valid_query_as_payload() throws Exception {
		client.type("application/sql-query");
		final Response response = client.post(VALID_QUERY);
		verify(response);
	}

	@Test
	public void bad_query_parameter() throws Exception {
		client.query(PARAM_QUERY, "");
		final Response response = client.get();
		assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
	}

	private void verify(final Response response) {
		assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
		assertEquals(APPLICATION_XML_TYPE, response.getMediaType());
	}
}
