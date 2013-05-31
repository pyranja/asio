package at.ac.univie.isc.asio.acceptance;

import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.univie.isc.asio.JaxrsClientProvider;
import at.ac.univie.isc.asio.sql.H2Provider;
import at.ac.univie.isc.asio.sql.KeyedRow;
import at.ac.univie.isc.asio.transfer.UpdateResult;

import com.google.common.collect.ImmutableMap;

@Category(FunctionalTest.class)
public class SqlUpdateTest {

	private static final URI SERVER_URL = URI
			.create("http://localhost:8080/v1/asio/update");

	// asio
	private static final String PARAM_UPDATE = "update";
	private static final String APPLICATION_SQL_UPDATE = "application/sql-update";

	// sql
	private static final String MOD_TABLE = "PATIENT";
	private static final String INSERT = "INSERT INTO " + MOD_TABLE
			+ " VALUES(42, 'test-name')";

	private WebClient client;
	private Response response;

	@Rule public JaxrsClientProvider provider = new JaxrsClientProvider(
			SERVER_URL);

	@Before
	public void setUp() throws SQLException {
		clear();
		client = provider.getClient();
	}

	@After
	public void tearDown() throws SQLException {
		clear();
	}

	@Test
	public void insert_as_form_param() throws Exception {
		final Form values = new Form();
		values.set(PARAM_UPDATE, INSERT);
		response = client.accept(APPLICATION_XML_TYPE).form(values);
		verify(response);
		wasInserted();
	}

	@Test
	public void insert_as_payload() throws Exception {
		response = client.accept(APPLICATION_XML_TYPE)
				.type(APPLICATION_SQL_UPDATE).post(INSERT);
		verify(response);
		wasInserted();
	}

	@Test
	public void bad_query_parameter() throws Exception {
		final Form values = new Form();
		values.set(PARAM_UPDATE, "");
		response = client.accept(APPLICATION_XML_TYPE).post(values);
		assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
	}

	@Test
	public void inacceptable_media_type() throws Exception {
		response = client.accept(MediaType.valueOf("test/notexisting"))
				.type(APPLICATION_SQL_UPDATE).post("test-update");
		assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
	}

	private void verify(final Response response2) {
		assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
		assertTrue(APPLICATION_XML_TYPE.isCompatible(response.getMediaType()));
		final UpdateResult result = JAXB.unmarshal(
				(InputStream) response.getEntity(), UpdateResult.class);
		assertEquals(INSERT, result.getCommand());
		assertEquals(1, result.getCount());
	}

	private void wasInserted() {
		final Map<String, KeyedRow> expected = ImmutableMap.of("42",
				new KeyedRow("42", Arrays.asList("42", "test-name")));
		final Map<String, KeyedRow> actual = ReferenceQuery.forQuery(
				"SELECT * FROM " + MOD_TABLE, "ID").getReference();
		assertEquals(expected, actual);
	}

	// clear the update test integration table
	private void clear() throws SQLException {
		try (Connection conn = H2Provider.connect()) {
			final Statement stmt = conn.createStatement();
			stmt.execute("DELETE FROM " + MOD_TABLE);
			stmt.close();
		}
	}
}
