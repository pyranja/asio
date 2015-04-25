package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.web.CaptureHttpExchange;
import at.ac.univie.isc.asio.web.HttpServer;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.sse.EventSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PigeonTest {
  private final CaptureHttpExchange endpoint = CaptureHttpExchange.create()
      .delegateTo(HttpServer.noContent());

  @Rule
  public final HttpServer http = HttpServer.create("asio-fake").with("/", endpoint);

  private final Client client = ClientBuilder.newClient();

  @After
  public void cleanUpClient() {
    client.close();
  }

  private Pigeon subject;

  @Before
  public void connect() {
    subject = Pigeon.connectTo(client.target(http.address()));
  }

  // === deploy

  @Test
  public void should_put_container_config() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_OK));
    final byte[] mapping = Payload.randomWithLength(128);
    subject.deploy(Id.valueOf("test"), mapping);
    assertThat(endpoint.single().getRequestURI(), equalTo(URI.create("/api/container/test")));
    assertThat(endpoint.single().getRequestMethod(), equalTo("PUT"));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Accept", Collections.singletonList("application/json")));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Content-type", Collections.singletonList("text/turtle")));
    assertThat(endpoint.singleRequestBody(), equalTo(mapping));
  }

  @Test(expected = Pigeon.ServerCommunicationFailure.class)
  public void should_fail_on_error_response_status() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_BAD_REQUEST));
    subject.deploy(Id.valueOf("test"), Payload.randomWithLength(32));
  }

  // === undeploy

  @Test
  public void should_delete_container_resource() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_OK));
    subject.undeploy(Id.valueOf("test"));
    assertThat(endpoint.single().getRequestURI(), equalTo(URI.create("/api/container/test")));
    assertThat(endpoint.single().getRequestMethod(), equalTo("DELETE"));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Accept", Collections.singletonList("application/json")));
  }

  @Test
  public void should_yield_true_if_container_was_destroyed() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_OK));
    assertThat(subject.undeploy(Id.valueOf("test")), equalTo(true));
  }

  @Test
  public void should_yield_false_if_no_container_was_found() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_NOT_FOUND));
    assertThat(subject.undeploy(Id.valueOf("test")), equalTo(false));
  }

  @Test(expected = Pigeon.ServerCommunicationFailure.class)
  public void should_fail_on_unexpected_http_status() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
    subject.undeploy(Id.valueOf("test"));
  }

  // === trace

  @Test
  public void should_yield_event_source_that_is_not_open_yet() throws Exception {
    assertThat(subject.eventStream().isOpen(), equalTo(false));
  }
  
  @Test
  public void should_subscribe_to_event_endpoint() throws Exception {
    final EventSource events = subject.eventStream();
    try {
      events.open();
    } catch (final Exception ignored) {
    } finally {
      events.close();
    }
    assertThat(endpoint.single().getRequestURI(), equalTo(URI.create("/api/events")));
    assertThat(endpoint.single().getRequestMethod(), equalTo("GET"));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Accept", Collections.singletonList("text/event-stream")));
  }

  // === health

  @Test
  public void should_query_health_endpoint() throws Exception {
    subject.health();
    assertThat(endpoint.single().getRequestURI(), equalTo(URI.create("/explore/insight/health")));
    assertThat(endpoint.single().getRequestMethod(), equalTo("GET"));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Accept", Collections.singletonList("application/json")));
  }

  @Test
  public void should_report_received_server_status() throws Exception {
    endpoint.delegateTo(HttpServer.jsonContent(HttpStatus.SC_OK, "{\"status\": \"UP\"}"));
    assertThat(subject.health(), equalTo(ServerStatus.UP));
  }

  @Test
  public void should_report_server_down_on_unexpected_response_code() throws Exception {
    assertThat(subject.health(), equalTo(ServerStatus.DOWN));
  }

  @Test
  public void should_report_server_down_on_unexpected_content() throws Exception {
    endpoint.delegateTo(HttpServer.jsonContent(HttpStatus.SC_OK, "{\"test\": 1}"));
    assertThat(subject.health(), equalTo(ServerStatus.DOWN));
  }

  @Test
  public void should_report_server_down_missing_content() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_OK));
    assertThat(subject.health(), equalTo(ServerStatus.DOWN));
  }

  @Test
  public void should_report_server_down_if_request_failed() throws Exception {
    endpoint.delegateTo(HttpServer.jsonContent(HttpStatus.SC_OK, "gaga1234"));
    assertThat(subject.health(), equalTo(ServerStatus.DOWN));
  }

  @Test
  public void should_report_server_down_if_not_reachable() throws Exception {
    http.close();
    assertThat(subject.health(), equalTo(ServerStatus.DOWN));
  }

  // === container listing

  @Test
  public void should_query_container_endpoint() throws Exception {
    subject.activeContainer();
    assertThat(endpoint.single().getRequestURI(), equalTo(URI.create("/api/container")));
    assertThat(endpoint.single().getRequestMethod(), equalTo("GET"));
    assertThat(endpoint.single().getRequestHeaders(), hasEntry("Accept", Collections.singletonList("application/json")));
  }

  @Test
  public void should_yield_deployed_container_ids() throws Exception {
    endpoint.delegateTo(HttpServer.jsonContent(HttpStatus.SC_OK, "[\"first\", \"second\"]"));
    assertThat(subject.activeContainer(), hasItems(Id.valueOf("first"), Id.valueOf("second")));
  }

  @Test
  public void should_yield_empty_list_if_container_endpoint_not_found() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_NOT_FOUND));
    assertThat(subject.activeContainer(), empty());
  }

  @Test(expected = Pigeon.ServerCommunicationFailure.class)
  public void should_throw_CommFailure_if_request_fails() throws Exception {
    endpoint.delegateTo(HttpServer.fixedStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR));
    subject.activeContainer();
  }
}
