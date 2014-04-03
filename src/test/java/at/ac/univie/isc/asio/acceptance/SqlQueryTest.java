package at.ac.univie.isc.asio.acceptance;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import at.ac.univie.isc.asio.converter.CsvToMap;
import at.ac.univie.isc.asio.converter.ResultSetToMap;
import at.ac.univie.isc.asio.sql.KeyedRow;
import at.ac.univie.isc.asio.tool.FunctionalTest;


@Category(FunctionalTest.class)
public class SqlQueryTest extends AcceptanceHarness {

  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM person";
  private static final String SCAN_QUERY = "SELECT * FROM person";
  private static final String SCAN_PK_LABEL = "ID";

  private static ReferenceQuery EXPECTED;

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sql");
  }

  @BeforeClass
  public static void fetchReferenceData() {
    EXPECTED = ReferenceQuery.forQuery(SCAN_QUERY, SCAN_PK_LABEL);
  }

  @Test
  public void valid_query_as_uri_param() throws Exception {
    client.accept(XML).query(PARAM_QUERY, COUNT_QUERY);
    response = client.get();
    verify(response);
  }

  @Test
  public void valid_query_as_form_param() throws Exception {
    client.accept(XML);
    final Form values = new Form();
    values.set(PARAM_QUERY, COUNT_QUERY);
    response = client.form(values);
    verify(response);
  }

  @Test
  public void valid_query_as_payload() throws Exception {
    client.accept(XML).type("application/sql-query");
    response = client.post(COUNT_QUERY);
    verify(response);
  }

  @Test
  public void delivers_csv() throws Exception {
    client.accept(CSV).query(PARAM_QUERY, SCAN_QUERY);
    response = client.get();
    assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
    assertTrue(CSV.isCompatible(response.getMediaType()));
    final Map<String, KeyedRow> results =
        CsvToMap.convertStream((InputStream) response.getEntity(), SCAN_PK_LABEL);
    assertEquals("response body not matching expected query result", EXPECTED.getReference(),
        results);
  }

  @Test
  public void delivers_xml() throws Exception {
    client.accept(XML).query(PARAM_QUERY, SCAN_QUERY);
    response = client.get();
    assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
    assertTrue(XML.isCompatible(response.getMediaType()));
    final Map<String, KeyedRow> results =
        ResultSetToMap.convertStream((InputStream) response.getEntity(), SCAN_PK_LABEL);
    assertEquals("response body not matching expected query result", EXPECTED.getReference(),
        results);
  }

  @Test
  public void bad_query_parameter() throws Exception {
    client.accept(XML).query(PARAM_QUERY, "");
    response = client.get();
    assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
  }

  @Test
  public void inacceptable_media_type() throws Exception {
    client.accept(MediaType.valueOf("test/notexisting")).query(PARAM_QUERY, COUNT_QUERY);
    response = client.get();
    assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
  }

  private void verify(final Response response) {
    assertEquals(SUCCESSFUL, familyOf(response.getStatus()));
    assertTrue(XML.isCompatible(response.getMediaType()));
  }
}