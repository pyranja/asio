package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.insight.EventStream;
import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.Payload;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static at.ac.univie.isc.asio.insight.Events.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FeatureContainerManagement extends IntegrationTest {

  @Before
  public void skipIfNotSupported() {
    ensureContainerSupported();
  }

  // @formatter:off

  @Test
  public void lists_deployed_containers() throws Exception {
    given().role("admin").manage().and()
      .when()
        .get("container")
      .then()
        .statusCode(HttpStatus.SC_OK)
        .body("", not(empty()));
  }

  @Test
  public void emits_events_on_container_deployment_and_dropping() throws Exception {
    final String name = UUID.randomUUID().toString();
    final byte[] mapping = Payload.asArray(Classpath.load("config.integration.ttl"));

    final Iterable<InboundEvent> received = EventStream.collectAll(eventStream().filter(only("container", "error")).take(2));

    given().manage().and().contentType("text/turtle").content(mapping)
      .when().put("container/{name}", name)
      .then().statusCode(HttpStatus.SC_CREATED);
    given().manage().spec()
      .when().delete("container/{name}", name)
      .then().statusCode(HttpStatus.SC_OK);

    assertThat(received, both(sequence("deployed", "dropped")).and(not(correlated())));
  }
}
