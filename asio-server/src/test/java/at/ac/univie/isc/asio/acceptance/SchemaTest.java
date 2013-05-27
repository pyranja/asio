package at.ac.univie.isc.asio.acceptance;

import static at.ac.univie.isc.asio.TestUtils.assertEqualsIgnoreCase;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.sql.Types;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import uk.org.ogsadai.converters.databaseschema.ColumnMetaData;
import uk.org.ogsadai.converters.databaseschema.DatabaseSchemaMetaData;
import uk.org.ogsadai.converters.databaseschema.RelationalSchemaParseException;
import uk.org.ogsadai.converters.databaseschema.TableMetaData;
import uk.org.ogsadai.converters.databaseschema.fromxml.XMLSchemaConverter;
import uk.org.ogsadai.util.xml.XML;
import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.univie.isc.asio.JaxrsClientProvider;

import com.google.common.base.Charsets;

@Category(FunctionalTest.class)
public class SchemaTest {

	private static final URI SERVER_URL = URI
			.create("http://localhost:8080/v1/asio/schema");

	private static final MediaType XML_TYPE = MediaType.APPLICATION_XML_TYPE
			.withCharset(Charsets.UTF_8.name());

	private WebClient client;
	private Response response;

	@Rule public JaxrsClientProvider provider = new JaxrsClientProvider(
			SERVER_URL);

	@Before
	public void setUp() {
		client = provider.getClient();
	}

	@Test
	public void delivers_schema_as_xml() throws Exception {
		response = client.accept(XML_TYPE).get();
		assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
		assertTrue(APPLICATION_XML_TYPE.isCompatible(response.getMediaType()));
		final DatabaseSchemaMetaData schema = parse(response);
		verifySchema(schema);
	}

	private DatabaseSchemaMetaData parse(final Response response) {
		try (Reader text = new InputStreamReader(
				(InputStream) response.getEntity(), Charsets.UTF_8)) {
			final InputSource xmlSource = new InputSource(text);
			final Document schema = XML.toDocument(xmlSource);
			return XMLSchemaConverter.convert(schema.getDocumentElement());
		} catch (final IOException | RelationalSchemaParseException e) {
			throw new AssertionError("parsing schema failed", e);
		}
	}

	private void verifySchema(final DatabaseSchemaMetaData schema) {
		@SuppressWarnings("unchecked")
		final Map<String, TableMetaData> tables = schema.getTables();
		assertEquals(2, tables.size()); // change this if schema changes
		final TableMetaData table = tables.get("PERSON");
		assertEqualsIgnoreCase("TEST", table.getCatalogName());
		assertEquals(5, table.getColumnCount());
		ColumnMetaData column = table.getColumn(1);
		assertEqualsIgnoreCase("id", column.getName());
		assertEquals(Types.INTEGER, column.getDataType());
		column = table.getColumn(2);
		assertEqualsIgnoreCase("firstname", column.getName());
		assertEquals(Types.VARCHAR, column.getDataType());
		column = table.getColumn(3);
		assertEqualsIgnoreCase("lastname", column.getName());
		assertEquals(Types.VARCHAR, column.getDataType());
		column = table.getColumn(4);
		assertEqualsIgnoreCase("age", column.getName());
		assertEquals(Types.VARCHAR, column.getDataType());
		column = table.getColumn(5);
		assertEqualsIgnoreCase("postalcode", column.getName());
		assertEquals(Types.VARCHAR, column.getDataType());
	}
}
