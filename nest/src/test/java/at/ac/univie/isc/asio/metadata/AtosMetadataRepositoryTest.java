package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Unchecked;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.web.CaptureHttpExchange;
import at.ac.univie.isc.asio.web.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import net.atos.*;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import rx.observers.TestSubscriber;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(HierarchicalContextRunner.class)
public class AtosMetadataRepositoryTest {
  @Rule
  public final Timeout timeout = Rules.timeout(3, TimeUnit.SECONDS);
  @Rule
  public final ExpectedException error = ExpectedException.none();
  @Rule
  public final Interactions interactions = Rules.interactions();
  @Rule
  public final HttpServer http = interactions.attached(Rules.httpServer("atos-metadata-stub"));

  private final Client client = ClientBuilder.newBuilder()
      .property(ClientProperties.CONNECT_TIMEOUT, 500)
      .property(ClientProperties.READ_TIMEOUT, 1000)
      .build();
  private final CaptureHttpExchange exchanges = CaptureHttpExchange.create();
  private final TestSubscriber<AtosDataset> subscriber = new TestSubscriber<>();

  private AtosMetadataRepository subject;

  @Before
  public void createRepository() {
    final WebTarget endpoint = client.target(http.address());
    subject = new AtosMetadataRepository(endpoint);
  }

  @After
  public void closeClient() {
    client.close();
  }

  @Test
  public void should_timeout_if_server_does_not_respond() throws Exception {
    http.with("/", new CaptureHttpExchange() {
      @Override
      protected void doRespond(final HttpExchange exchange) throws IOException {
        Unchecked.sleep(5, TimeUnit.SECONDS);
      }
    });
    error.expect(RepositoryFailure.class);
    subject.findOne("global-id").toBlocking().single();
  }

  public class FindOne {
    /*
     * the atos service seems to require UUIDs as global id
     */
    @Test
    public void should_fetch_from_resource_uri_with_globalId() throws Exception {
      http.with("/", exchanges);
      subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").subscribe(subscriber);
      final HttpExchange captured = exchanges.single();
      assertThat(captured.getRequestURI().getPath(), is("/metadata/349c9448-310c-4b2b-bd65-621db0950042"));
    }

    @Test
    public void should_send_GET_request() throws Exception {
      http.with("/", exchanges);
      subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").subscribe(subscriber);
      assertThat(exchanges.single().getRequestMethod(), is("GET"));
    }

    @Test
    public void should_request_xml_response() throws Exception {
      http.with("/", exchanges);
      subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").subscribe(subscriber);
      assertThat(exchanges.single().getRequestHeaders(), hasEntry("Accept", Arrays.asList("application/xml")));
    }

    @Test
    public void should_succeed_if_single_matching_dataset_found() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-dataset_resource.xml"));
      final AtosDataset expected = new AtosDataset()
          .withAuthor("author")
          .withCategory("category")
          .withDescription("description")
          .withLicence("license")
          .withGlobalID("349c9448-310c-4b2b-bd65-621db0950042")
          .withLocalID("http://example.com/local-id/")
          .withLinkedTo(new AtosLink().withLinkID("link-id").withLinkType("link-type").withLinkURI("link-uri"))
          .withCreationDate("2014-04-14 16:49:54.000")
          .withUpdateDate("2014-04-14 17:02:53.000")
          .withMetadataCreationDate("2014-04-14 16:49:54.945")
          .withMetadataUpdateDate("2015-02-23 11:41:51.307")
          .withName("name")
          .withProvenance("provenance")
          .withRating("0")
          .withRelatedResources(new AtosRelatedResource().withDescription("related-resource-description").withResourceID("related-resource-id"))
          .withResourceURL("http://example.com/resource-url/")
          .withSemanticAnnotations(new AtosSemanticConcept().withConceptURI("concept-uri").withLabel("concept-label"))
          .withStatus("active")
          .withTags("tag-one, tag-two")
          .withType("Dataset")
          .withViews("18")
          .withSparqlEndpoint("http://example.com/sparql-endpoint");
      final AtosDataset actual = subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").toBlocking().single();
      assertThat(actual, is(expected));
    }

    @Test
    public void should_yield_empty_if_no_dataset_with_matching_id_found() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-empty_dataset_resource.xml"));
      final List<AtosDataset> found = subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").toList().toBlocking().single();
      assertThat(found, is(empty()));
    }

    @Test
    public void should_interpret_internal_error_response_as_missing() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
      final List<AtosDataset> found = subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").toList().toBlocking().single();
      assertThat(found, is(empty()));
    }

    @Test
    public void should_interpret_not_found_response_as_missing() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_NOT_FOUND));
      final List<AtosDataset> found = subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").toList().toBlocking().single();
      assertThat(found, is(empty()));
    }

    @Test
    public void should_fail_on_unexpected_error_response() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_SERVICE_UNAVAILABLE));
      error.expect(RepositoryFailure.class);
      subject.findOne("349c9448-310c-4b2b-bd65-621db0950042").toBlocking().first();
    }
  }

  public class FindByLocalId {
    @Test
    public void should_query_for_given_localid() throws Exception {
      http.with("/", exchanges);
      subject.findByLocalId("http://example.com/test-id").subscribe(subscriber);
      final HttpExchange captured = exchanges.single();
      final Map<String, String> parameters = HttpServer.parseParameters(captured);
      assertThat(captured.getRequestURI().getPath(), is("/metadata/facets/Dataset/localID"));
      assertThat(parameters, hasEntry("value", "http://example.com/test-id"));
    }

    @Test
    public void should_send_GET_request() throws Exception {
      http.with("/", exchanges);
      subject.findByLocalId("http://example.com/test-id").subscribe(subscriber);
      assertThat(exchanges.single().getRequestMethod(), is("GET"));
    }

    @Test
    public void should_request_xml_response() throws Exception {
      http.with("/", exchanges);
      subject.findByLocalId("http://example.com/test-id").subscribe(subscriber);
      assertThat(exchanges.single().getRequestHeaders(), hasEntry("Accept", Arrays.asList("application/xml")));
    }

    @Test
    public void should_succeed_if_single_matching_dataset_found() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-single_dataset.xml"));
      final AtosDataset expected = new AtosDataset()
          .withAuthor("author")
          .withCategory("category")
          .withDescription("description")
          .withLicence("license")
          .withGlobalID("44bab742-2c27-4dad-8419-fae8a848358b")
          .withLocalID("http://example.com/local-id/")
          .withLinkedTo(new AtosLink().withLinkID("link-id").withLinkType("link-type").withLinkURI("link-uri"))
          .withCreationDate("2014-04-14 16:49:54.000")
          .withUpdateDate("2014-04-14 17:02:53.000")
          .withMetadataCreationDate("2014-04-14 16:49:54.945")
          .withMetadataUpdateDate("2015-02-23 11:41:51.307")
          .withName("name")
          .withProvenance("provenance")
          .withRating("0")
          .withRelatedResources(new AtosRelatedResource().withDescription("related-resource-description").withResourceID("related-resource-id"))
          .withResourceURL("http://example.com/resource-url/")
          .withSemanticAnnotations(new AtosSemanticConcept().withConceptURI("concept-uri").withLabel("concept-label"))
          .withStatus("active")
          .withTags("tag-one, tag-two")
          .withType("Dataset")
          .withViews("18")
          .withSparqlEndpoint("http://example.com/sparql-endpoint");
      final AtosDataset actual = subject.findByLocalId("http://example.com/test-id").toBlocking().single();
      assertThat(actual, is(expected));
    }

    @Test
    public void should_yield_empty_if_no_dataset_with_matching_id_found() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-no_dataset.xml"));
      final List<AtosDataset> found = subject.findByLocalId("http://example.com/test-id").toList().toBlocking().single();
      assertThat(found, is(empty()));
    }

    @Test
    public void should_yield_empty_on_missing_resource_metadata() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-empty_resource_metadata.xml"));
      final List<AtosDataset> found = subject.findByLocalId("http://example.com/test-id").toList().toBlocking().single();
      assertThat(found, is(empty()));
    }

    @Test
    public void should_yield_all_datasets_with_matching_id() throws Exception {
      http.with("/", StaticHandler.fromClasspath("atos-metadata/AtosMetadataRepositoryTest-multiple_dataset.xml"));
      final List<AtosDataset> found = subject.findByLocalId("http://example.com/test-id").toList().toBlocking().single();
      assertThat(found, hasSize(2));
    }

    @Test
    public void should_fail_on_error_response() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
      error.expect(RepositoryFailure.class);
      error.expectCause(Matchers.<Throwable>instanceOf(InternalServerErrorException.class));
      subject.findByLocalId("http://example.com/test-id").toBlocking().single();
    }
  }

  public class Save {
    /*
     * Similar to JPA if entity id (globalID) is not set, create a new metadata in the repository
     * and assign the generated id.
     */

    private final AtosDataset dataset = new AtosDataset()
        .withGlobalID(null)   // null globalID triggers save
        .withName("saved dataset")
        .withLocalID("http://example.com/local");

    @Test
    public void should_send_POST_request_to_generate_a_new_resource() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestMethod(), is("POST"));
    }

    @Test
    public void should_post_new_data_to_service_root() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestURI().getPath(), is("/metadata"));
    }

    @Test
    public void should_request_xml_response() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestHeaders(), hasEntry("Accept", Arrays.asList("application/xml")));
    }

    @Test
    public void should_send_serialized_metadata() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      final AtosResourceMetadata
          sent = JAXB.unmarshal(new ByteArrayInputStream(exchanges.singleRequestBody()), AtosResourceMetadata.class);
      assertThat(sent, is(new AtosResourceMetadata().withDataset(dataset)));
    }

    @Test
    public void should_fail_if_response_not_success() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
      error.expect(RepositoryFailure.class);
      error.expectCause(Matchers.<Throwable>instanceOf(InternalServerErrorException.class));
      subject.save(dataset).toBlocking().single();
    }
  }

  public class Update {
    /*
     * If entity id (globalID) is set, attempt to modify an existing metadata resource.
     */
    private final AtosDataset dataset = new AtosDataset()
        .withGlobalID("existing-id")
        .withName("updated dataset")
        .withLocalID("http://example.com/local");

    @Test
    public void should_send_PUT_request_to_update_a_resource() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestMethod(), is("PUT"));
    }

    @Test
    public void should_send_update_to_resource_uri() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestURI().getPath(), is("/metadata/" + dataset.getGlobalID()));
    }

    @Test
    public void should_request_xml_response() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      assertThat(exchanges.single().getRequestHeaders(), hasEntry("Accept", Arrays.asList("application/xml")));
    }

    @Test
    public void should_send_serialized_metadata() throws Exception {
      http.with("/", exchanges);
      subject.save(dataset).subscribe(subscriber);
      final AtosResourceMetadata
          sent = JAXB.unmarshal(new ByteArrayInputStream(exchanges.singleRequestBody()), AtosResourceMetadata.class);
      assertThat(sent, is(new AtosResourceMetadata().withDataset(dataset)));
    }

    @Test
    public void should_fail_if_response_not_success() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
      error.expect(RepositoryFailure.class);
      error.expectCause(Matchers.<Throwable>instanceOf(InternalServerErrorException.class));
      subject.save(dataset).toBlocking().single();
    }
  }

  public class Delete {
    @Test
    public void should_send_DELETE_request_to_remove_a_resource() throws Exception {
      http.with("/", exchanges);
      subject.delete("349c9448-310c-4b2b-bd65-621db0950042").subscribe(new TestSubscriber<String>());
      assertThat(exchanges.single().getRequestMethod(), is("DELETE"));
    }

    @Test
    public void should_send_deletion_to_resource_uri() throws Exception {
      http.with("/", exchanges);
      subject.delete("349c9448-310c-4b2b-bd65-621db0950042").subscribe(new TestSubscriber<String>());
      assertThat(exchanges.single().getRequestURI().getPath(), is("/metadata/349c9448-310c-4b2b-bd65-621db0950042"));
    }

    @Test
    public void should_request_xml_response() throws Exception {
      http.with("/", exchanges);
      subject.delete("349c9448-310c-4b2b-bd65-621db0950042").subscribe(new TestSubscriber<String>());
      assertThat(exchanges.single().getRequestHeaders(), hasEntry("Accept", Arrays.asList("application/xml")));
    }

    @Test
    public void should_yield_identifier_on_success() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_OK));
      final String yielded = subject.delete("349c9448-310c-4b2b-bd65-621db0950042").toBlocking().single();
      assertThat(yielded, is("349c9448-310c-4b2b-bd65-621db0950042"));
    }

    @Test
    public void should_fail_if_response_not_success() throws Exception {
      http.with("/", CaptureHttpExchange.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
      error.expect(RepositoryFailure.class);
      error.expectCause(Matchers.<Throwable>instanceOf(InternalServerErrorException.class));
      subject.delete("349c9448-310c-4b2b-bd65-621db0950042").toBlocking().single();
    }
  }

  static class StaticHandler extends CaptureHttpExchange {
    private final byte[] response;

    private StaticHandler(final byte[] response) {
      this.response = response;
    }

    static StaticHandler fromJaxbBean(final Object entity) {
      final ByteArrayOutputStream content = new ByteArrayOutputStream();
      JAXB.marshal(entity, content);
      return new StaticHandler(content.toByteArray());
    }

    static StaticHandler fromClasspath(final String pathToResponse) throws IOException {
      return new StaticHandler(Classpath.load(pathToResponse).read());
    }

    @Override
    protected void doRespond(final HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/xml");
      exchange.sendResponseHeaders(HttpStatus.SC_OK, response.length);
      exchange.getResponseBody().write(response);
    }
  }
}
