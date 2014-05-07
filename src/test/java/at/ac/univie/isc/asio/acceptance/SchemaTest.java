package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.base.Charsets;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import uk.org.ogsadai.converters.databaseschema.ColumnMetaData;
import uk.org.ogsadai.converters.databaseschema.DatabaseSchemaMetaData;
import uk.org.ogsadai.converters.databaseschema.RelationalSchemaParseException;
import uk.org.ogsadai.converters.databaseschema.TableMetaData;
import uk.org.ogsadai.converters.databaseschema.fromxml.XMLSchemaConverter;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.sql.Types;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class SchemaTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sql/schema");
  }

  @Test
  public void delivers_schema_as_xml() throws Exception {
    response = client.accept(XML).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(XML.isCompatible(response.getMediaType()), is(true));
    final DatabaseSchemaMetaData schema = parse(response);
    verifySchema(schema);
  }

  private DatabaseSchemaMetaData parse(final Response response) {
    try (Reader text = new InputStreamReader((InputStream) response.getEntity(), Charsets.UTF_8)) {
      final InputSource xmlSource = new InputSource(text);
      final Document schema = uk.org.ogsadai.util.xml.XML.toDocument(xmlSource);
      return XMLSchemaConverter.convert(schema.getDocumentElement());
    } catch (final IOException | RelationalSchemaParseException e) {
      throw new AssertionError("parsing schema failed", e);
    }
  }

  // XXX : this depends on test schema !
  private void verifySchema(final DatabaseSchemaMetaData schema) {
    @SuppressWarnings("unchecked")
    final Map<String, TableMetaData> tables = schema.getTables();
    assertThat(tables.size(), is(4));
    final TableMetaData table = tables.get("PERSON");
    assertThat(table.getCatalogName(), is(equalToIgnoringCase("TEST")));
    assertEquals(5, table.getColumnCount());
    ColumnMetaData column = table.getColumn(1);
    assertThat(column.getName(), is(equalToIgnoringCase("id")));
    assertEquals(Types.INTEGER, column.getDataType());
    column = table.getColumn(2);
    assertThat(column.getName(), is(equalToIgnoringCase("firstname")));
    assertEquals(Types.VARCHAR, column.getDataType());
    column = table.getColumn(3);
    assertThat(column.getName(), is(equalToIgnoringCase("lastname")));
    assertEquals(Types.VARCHAR, column.getDataType());
    column = table.getColumn(4);
    assertThat(column.getName(), is(equalToIgnoringCase("age")));
    assertEquals(Types.VARCHAR, column.getDataType());
    column = table.getColumn(5);
    assertThat(column.getName(), is(equalToIgnoringCase("postalcode")));
    assertEquals(Types.VARCHAR, column.getDataType());
  }
}
