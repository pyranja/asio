package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.tool.Reactive;
import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Ensure the AtosMetadataRepository works with the actual endpoint.
 */
@org.junit.experimental.categories.Category(IntegrationTest.class)
public class AtosMetadataIntegrationTest {
  private static final URI ATOS_SERVICE_ADDRESS =
      URI.create("http://vphshare.atosresearch.eu/metadata-extended/rest");

  private final Client client = ClientBuilder.newBuilder()
      .property(ClientProperties.CONNECT_TIMEOUT, 2_000)
      .property(ClientProperties.READ_TIMEOUT, 10_000)
          //      .register(new LoggingFilter(Logger.getLogger("jersey.client"), true))
      .build();

  private final AtosMetadataRepository repo =
      new AtosMetadataRepository(client.target(ATOS_SERVICE_ADDRESS));

  private final AtosDataset dataset = new AtosDataset()
      .withName("asio-test")
      .withType("Dataset")
      .withAuthor("Chris Borckholder")
      .withCategory("Testing")
      .withDescription("Fictional metadata for integration testing.")
      .withLicence("public domain")
      .withProvenance("test-provenance")
      .withLocalID("http://example.com/test/local-id/")
      .withStatus("non active")
      .withCreationDate("2015-03-01 16:58:54.000")
      .withUpdateDate("2015-03-03 17:02:53.000")
      .withRating("0")
      .withViews("18")
      .withTags("tag-one, tag-two")
      .withResourceURL("http://example.com/test/resource")
      .withSparqlEndpoint("http://example.com/test/sparql")
      .withLinkedTo(new AtosLink().withLinkID("myLink").withLinkType("test").withLinkURI("http://example.com/test/link"))
      .withRelatedResources(new AtosRelatedResource().withDescription("related-resource-description").withResourceID("related-resource-id"))
      .withSemanticAnnotations(new AtosSemanticConcept().withConceptURI("concept-uri").withLabel("concept-label"));

  @Before
  public void setUp() throws Exception {
    // skip if atos service is down
    assumeThat(client.target(ATOS_SERVICE_ADDRESS).path("metadata").request().get(), hasStatus(Response.Status.OK));
  }

  @After
  public void closeClient() throws Exception {
    repo.findByLocalId("http://example.com/test/local-id/")
        .subscribe(new Action1<AtosDataset>() {
          @Override
          public void call(final AtosDataset atosDataset) {
            repo.delete(atosDataset.getGlobalID());
          }
        }, Reactive.ignoreErrors());
    client.close();
  }

  @Test
  public void create_and_delete_a_dataset() throws Exception {
    final Boolean wasDeleted = repo.save(dataset)
        .flatMap(new Func1<AtosDataset, Observable<String>>() {
          @Override
          public Observable<String> call(final AtosDataset atosDataset) {
            return repo.delete(atosDataset.getGlobalID());
          }
        })
        .flatMap(new Func1<String, Observable<?>>() {
          @Override
          public Observable<?> call(final String identifier) {
            return repo.findOne(identifier);
          }
        })
        .isEmpty().toBlocking().single();
    assertThat(wasDeleted, is(true));
  }

  @Test
  public void create_update_then_delete() throws Exception {
    final AtosDataset saved = repo.save(dataset).toBlocking().single();
    try {
      final AtosDataset modified = (AtosDataset) saved.clone();
      modified.withProvenance("modified provenance");
      repo.save(modified).toBlocking().singleOrDefault(null);
      final AtosDataset retrieved = repo.findOne(saved.getGlobalID()).toBlocking().single();
      assertThat(retrieved.getProvenance(), is("modified provenance"));
    } finally {
      repo.delete(saved.getGlobalID());
    }
  }
}
