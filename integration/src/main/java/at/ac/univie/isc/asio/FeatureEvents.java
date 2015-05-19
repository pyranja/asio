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
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static at.ac.univie.isc.asio.insight.Events.*;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Emitting events from protocol operations.
 */
@SuppressWarnings("unchecked")
@Category(Integration.class)
@RunWith(Parameterized.class)
public class FeatureEvents extends IntegrationTest {

  @Parameterized.Parameters(name = "{index} : {0}-{1}")
  public static Iterable<Object[]> variants() {
    // { language, operation, noop_command }
    return Arrays.asList(new Object[][] {
        {"sql", "query", "SELECT 1"},
        {"sql", "update", "DROP TABLE IF EXISTS test_gaga_12345"},
        {"sparql", "query", "ASK {}"},
    });
  }

  @Parameterized.Parameter(0)
  public String language;
  @Parameterized.Parameter(1)
  public String operation;
  @Parameterized.Parameter(2)
  public String noop;

  @Before
  public void ensureLanguageSupported() {
    ensureLanguageSupported(language);
  }

  private String requestId;

  @Test
  public void emit_subscribed_on_connecting() throws Exception {
    final InboundEvent received = eventStream().take(1).toBlocking().single();
    assertThat(received, both(attribute("type", Matchers.<Object>equalTo("stream")))
        .and(attribute("subject", Matchers.<Object>equalTo("subscribed"))));
  }

  @Test
  public void successful_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().filter(only("operation", "error")).take(3));

    requestId = given().role("admin").and()
        .param(operation, noop).post("/{language}", language)
        .then().statusCode(is(HttpStatus.SC_OK)).extract().header("Correlation");

    assertThat(received, both(sequence("received", "executed", "completed")).and(correlated(requestId)));
  }

  @Test
  public void failed_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().filter(only("operation", "error")).take(3));

    final String invalidCommand = Hashing.md5().hashString(noop, Charsets.UTF_8).toString();
    requestId = given().role("admin").and()
        .param(operation, invalidCommand).post("/{language}", language)
        .then().statusCode(not(HttpStatus.SC_OK)).extract().header("Correlation");

    assertThat(received, both(sequence("received", "failed", "error")).and(correlated(requestId)));
  }

  @Test
  public void rejected_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().filter(only("operation", "error")).take(3));

    requestId = given().role("admin").and().param(operation).post("/{language}", language)
        .then().statusCode(not(HttpStatus.SC_OK)).extract().header("Correlation");

    assertThat(received, both(sequence("received", "failed", "error")).and(correlated(requestId)));
  }
}
