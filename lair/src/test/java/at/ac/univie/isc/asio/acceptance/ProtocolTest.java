package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.univie.isc.asio.jaxrs.Mime;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * Verify protocol behavior using dummy sql queries.
 */
@Category(FunctionalTest.class)
public class ProtocolTest extends AcceptanceHarness {

  public static final String NOOP_QUERY = "SELECT 1";

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sql");
  }

  // ====================================================================================>
  // GENERIC FUNCTIONALITY

  @Test
  public void override_requested_media_type_with_asio_query_parameter() throws Exception {
    response = client()
        .queryParam("query", NOOP_QUERY)
        .queryParam("x-asio-accept", Mime.CSV)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();
    assertThat(response.getMediaType(), is(compatibleTo(Mime.CSV.type())));
  }

  @Test
  public void override_requested_media_type_with_cxf_query_parameter() throws Exception {
    // .queryParam("_type", "xml") works also -> cxf built-in RequestPreprocessor
    response = client()
        .queryParam("query", NOOP_QUERY)
        .queryParam("_type", Mime.CSV)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();
    assertThat(response.getMediaType(), is(compatibleTo(Mime.CSV.type())));
  }

  // ====================================================================================>
  // ILLEGAL REQUESTS

  @Test
  public void should_respond_not_found_on_unsupported_language() throws Exception {
    response = client(readAccess()).path("unsupported-language").request().get();
    assertThat(response, hasStatus(Response.Status.NOT_FOUND));
  }

  @Test
  public void body_operation_with_illegal_content_type() throws Exception {
    response = client().request()
        .post(Entity.entity(NOOP_QUERY, MediaType.TEXT_PLAIN_TYPE));
    assertThat(response, hasStatus(Response.Status.UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  public void body_operation_with_language_mismatch_in_content_type() throws Exception {
    response = client().request()
        .post(Entity.entity(NOOP_QUERY, MediaType.valueOf("application/sparql-action")));
    assertThat(response, hasStatus(Response.Status.UNSUPPORTED_MEDIA_TYPE));
  }


}
