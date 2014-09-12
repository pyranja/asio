package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.Column;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class SchemaTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("meta/schema");
  }

  @Test
  public void delivers_schema_as_xml() throws Exception {
    response = client().request(Mime.XML.type()).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(Mime.XML.type().isCompatible(response.getMediaType()), is(true));
    response.readEntity(SqlSchema.class); // ignore actual content
  }

  @Test
  public void delivers_schema_as_json() throws Exception {
    response = client().request(Mime.JSON.type()).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(Mime.JSON.type().isCompatible(response.getMediaType()), is(true));
    response.readEntity(SqlSchema.class); // ignore actual content
  }

  @Test
  public void can_parse_xml_schema() throws Exception {
    final SqlSchema schema = client().request(Mime.XML.type()).get(SqlSchema.class);
    verify(schema);
  }

  @Test
  public void can_parse_json_schema() throws Exception {
    final SqlSchema schema = client().request(Mime.JSON.type()).get(SqlSchema.class);
    verify(schema);
  }

  private void verify(final SqlSchema schema) {
    final List<Table> tables = schema.getTable();
    // !! depends on test schema
    assertThat(tables.size(), is(5));
    assertThat(tables, hasItem(PATIENT_TABLE));
  }

  public static final Table PATIENT_TABLE = new Table()
      .withName("PATIENT")
      .withCatalog("TEST")
      .withSchema("PUBLIC")
      .withColumn(
          new Column()
              .withName("ID")
              .withType(QName.valueOf("{http://www.w3.org/2001/XMLSchema-instance}long"))
              .withSqlType("integer")
              .withLength(10)
          , new Column()
              .withName("NAME")
              .withType(QName.valueOf("{http://www.w3.org/2001/XMLSchema-instance}string"))
              .withSqlType("varchar")
              .withLength(255)
      );
}
