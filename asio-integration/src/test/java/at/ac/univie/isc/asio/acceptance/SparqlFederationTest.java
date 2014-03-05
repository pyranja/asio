package at.ac.univie.isc.asio.acceptance;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.HttpHeaders;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import at.ac.univie.isc.asio.tool.FunctionalTest;

import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.MoreExecutors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
@Category(FunctionalTest.class)
public class SparqlFederationTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sparql");
  }

  private HttpServer server;
  private MockHandler handler;

  private static class MockHandler implements HttpHandler {
    public final AtomicReference<HttpExchange> received = new AtomicReference<>();

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
      received.set(exchange);
      exchange.sendResponseHeaders(500, -1);
      exchange.close();
    }
  }

  @Before
  public void setUp() throws IOException {
    final InetSocketAddress address = new InetSocketAddress(1337);
    handler = new MockHandler();
    server = HttpServer.create(address, 10);
    server.createContext("/sparql", handler);
    server.setExecutor(MoreExecutors.sameThreadExecutor());
    server.start();
  }

  @After
  public void tearDown() {
    server.stop(0);
  }

  @Test
  public void should_execute_remote_request_on_federated_sparql_query() throws Exception {
    final String query = "SELECT * WHERE { SERVICE <http://localhost:1337/sparql> { ?s ?p ?o }}";
    client.accept(CSV).query(PARAM_QUERY, query);
    client.get(); // do not care for response
    assertThat(handler.received.get(), is(notNullValue()));
  }

  @Test
  public void should_delegate_credentials_to_remote_request_in_federation() throws Exception {
    final String query = "SELECT * WHERE { SERVICE <http://localhost:1337/sparql> { ?s ?p ?o }}";
    client.accept(CSV).query(PARAM_QUERY, query);
    // a mock vph token
    final String credentials = BaseEncoding.base64().encode(":test-password".getBytes());
    client.header(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
    client.get(); // do not care for response
    final String delegated =
        handler.received.get().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    assertThat(delegated, startsWith("Basic "));
    final String decoded = new String(BaseEncoding.base64().decode(delegated.substring(6)));
    assertThat(decoded, is(":test-password"));
  }
}
