package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.FunctionalTest;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static javax.ws.rs.core.Response.Status.Family.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


@Category(FunctionalTest.class)
public class SparqlQueryTest extends AcceptanceHarness {

  private static final String TEST_QUERY = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";
  public static final Escaper URL_ESCAPER = UrlEscapers.urlPathSegmentEscaper();

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sparql");
  }

  @Test
  public void valid_query_as_uri_param() throws Exception {
    // JAX-RS client api interprets { } (curly braces) as URI template
    //  => must escape query when using query parameters
    final String escaped = URL_ESCAPER.escape(TEST_QUERY);
    response = client().queryParam("query", escaped).request(Mime.CSV.type()).get();
    verify(response);
  }

  @Test
  public void valid_query_as_form_param() throws Exception {
    final Form values = new Form().param("query", TEST_QUERY);
    response = client().request(Mime.CSV.type()).post(Entity.form(values));
    verify(response);
  }

  @Test
  public void valid_query_as_payload() throws Exception {
    response = client().request(Mime.CSV.type()).post(Entity.entity(TEST_QUERY, Mime.QUERY_SPARQL.type()));
    verify(response);
  }

  @Test
  public void delivers_csv() throws Exception {
    response = client().request(Mime.CSV.type()).post(Entity.entity(TEST_QUERY, Mime.QUERY_SPARQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.CSV.type())));
  }

  @Test
  public void delivers_json() throws Exception {
    response = client().request(Mime.JSON.type()).post(Entity.entity(TEST_QUERY, Mime.QUERY_SPARQL.type()));
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.JSON.type())));
  }

  @Test
  public void bad_query_parameter() throws Exception {
    response = client().queryParam("query", "").request().get();
    assertThat(response, hasFamily(CLIENT_ERROR));
  }

  @Test
  public void unacceptable_media_type() throws Exception {
    response = client().request(MediaType.valueOf("test/notexisting"))
        .post(Entity.entity(TEST_QUERY, Mime.QUERY_SPARQL.type()));
    assertThat(response, hasStatus(Response.Status.NOT_ACCEPTABLE));
  }

  private void verify(final Response response) {
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.CSV.type())));
  }
}
