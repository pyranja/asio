/*
 * #%L
 * asio integration
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
  
  @Test
  public void redeployment_is_a_drop_deploy_sequence() throws Exception {
    // this test guards against a regression where redeploying with multi-tenancy enabled fails,
    // due to the reordering of mysql user creation on assembly and dropping the user while
    // destroying the former container instance
    final String name = UUID.randomUUID().toString();
    final byte[] mapping = Payload.asArray(Classpath.load("config.integration.ttl"));

    final Iterable<InboundEvent> received = EventStream.collectAll(eventStream().filter(only("container", "error")).take(4));

    given().manage().and().contentType("text/turtle").content(mapping)
      .when().put("container/{name}", name)
      .then().statusCode(HttpStatus.SC_CREATED);
    given().manage().and().contentType("text/turtle").content(mapping)
      .when().put("container/{name}", name)
      .then().statusCode(HttpStatus.SC_CREATED);
    given().manage().spec()
      .when().delete("container/{name}", name)
      .then().statusCode(HttpStatus.SC_OK);

    assertThat(received, both(sequence("deployed", "dropped", "deployed", "dropped")).and(not(correlated())));
  }
}
