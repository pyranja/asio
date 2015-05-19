/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.component;

import at.ac.univie.isc.asio.Integration;
import at.ac.univie.isc.asio.metadata.AtosMetadataRepository;
import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.logging.Logger;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Ensure the AtosMetadataRepository works with the actual endpoint.
 */
@Category(Integration.class)
public class AtosMetadataIntegrationTest {
  private static final URI ATOS_SERVICE_ADDRESS =
      URI.create("http://vphshare.atosresearch.eu/metadata-extended/rest");

  private final Client client = ClientBuilder.newBuilder()
      .property(ClientProperties.CONNECT_TIMEOUT, 2_000)
      .property(ClientProperties.READ_TIMEOUT, 30_000)
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
            repo.delete(atosDataset.getGlobalID()).subscribe(
                new Action1<String>() {
                  @Override
                  public void call(final String s) { /* noop */ }
                },
                new Action1<Throwable>() {
                  @Override
                  public void call(final Throwable throwable) {
                    System.err.println("!! failed to delete " + atosDataset.getGlobalID() + " - " + throwable);
                  }
                });
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(final Throwable throwable) {
            System.err.println("!! failed to clear test metadata - " + throwable);
          }
        });
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
      repo.delete(saved.getGlobalID()).subscribe();
    }
  }

  /**
   * run main to try and clean up all test metadata
   */
  public static void main(String[] args) {
    final Client client = ClientBuilder.newBuilder()
        .property(ClientProperties.CONNECT_TIMEOUT, 2_000)
        .property(ClientProperties.READ_TIMEOUT, 0)
        .register(new LoggingFilter(Logger.getLogger("jersey.client"), true))
        .build();
    try {
      final AtosMetadataRepository repo =
          new AtosMetadataRepository(client.target(ATOS_SERVICE_ADDRESS));
      repo.findByLocalId("http://example.com/test/local-id/")
          .subscribe(new Action1<AtosDataset>() {
            @Override
            public void call(final AtosDataset atosDataset) {
              System.out.println(">> delete " + atosDataset.getGlobalID());
              repo.delete(atosDataset.getGlobalID()).subscribe(
                  new Action1<String>() {
                    @Override
                    public void call(final String id) {
                      System.out.println("<< deleted " + id);
                    }
                  }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                      System.err.println("!! failed to delete " + atosDataset.getGlobalID() + " - " + throwable);
                    }
                  }
              );
            }
          }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable throwable) {
              System.err.println("!! failed to clear test metadata - " + throwable);
            }
          });
    } finally {
      client.close();
    }
  }
}
