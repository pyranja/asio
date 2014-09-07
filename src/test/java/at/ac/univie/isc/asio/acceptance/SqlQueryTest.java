package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.sql.ConvertToTable;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.collect.Table;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.WebRowSet;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.*;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@Category(FunctionalTest.class)
public class SqlQueryTest extends AcceptanceHarness {
  public static final Escaper URL_ESCAPER = UrlEscapers.urlPathSegmentEscaper();

  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM person";
  private static final String SCAN_QUERY = "SELECT * FROM person";

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sql");
  }

  @BeforeClass
  public static void fetchReferenceData() {
  }

  @Test
  public void valid_query_as_uri_param() throws Exception {
    // JAX-RS client api interprets { } (curly braces) as URI template
    //  => must escape query when using query parameters
    final String query = URL_ESCAPER.escape(COUNT_QUERY);
    response = client().queryParam("query", query).request(Mime.XML.type()).get();
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
  }

  @Test
  public void valid_query_as_form_param() throws Exception {
    final Form values = new Form().param("query", COUNT_QUERY);
    response = client().request(Mime.XML.type()).post(Entity.form(values));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
  }

  @Test
  public void valid_query_as_payload() throws Exception {
    response = client().request(Mime.XML.type()).post(Entity.entity(COUNT_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
  }

  @Test
  public void delivers_csv() throws Exception {
    response = client().request(Mime.CSV.type()).post(Entity.entity(SCAN_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.CSV.type())));
    final Table<Integer, String, String> results =
        ConvertToTable.fromCsv(response.readEntity(InputStream.class));
    final Table<Integer, String, String> expected = database().reference(SCAN_QUERY);
    assertThat(results, is(expected));
  }

  @Test
  public void delivers_xml() throws Exception {
    response = client().request(Mime.XML.type()).post(Entity.entity(SCAN_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
    final WebRowSet webRowSet = RowSetProvider.newFactory().createWebRowSet();
    webRowSet.readXml(response.readEntity(InputStream.class));
    final Table<Integer, String, String> results = ConvertToTable.fromResultSet(webRowSet);
    final Table<Integer, String, String> expected = database().reference(SCAN_QUERY);
    assertThat(results, is(expected));
  }

  @Test
  public void bad_query_parameter() throws Exception {
    response = client().queryParam("query", "").request(Mime.XML.type()).get();
    assertThat(response, hasFamily(CLIENT_ERROR));
  }

  @Test
  public void unacceptable_media_type() throws Exception {
    response = client().request(MediaType.valueOf("test/not-existing"))
        .post(Entity.entity(COUNT_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasStatus(Response.Status.NOT_ACCEPTABLE));
  }

  @Test
  public void delivers_xml_if_no_accepted_type_given() throws Exception {
    response = client().request().post(Entity.entity(COUNT_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
  }

  @Test
  public void delivers_xml_if_wildcard_accept_header_given() throws Exception {
    response = client().request(MediaType.WILDCARD_TYPE).post(Entity.entity(COUNT_QUERY, Mime.QUERY_SQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
  }
}
