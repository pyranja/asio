package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.basicAuthPassword;
import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.basicAuthUsername;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Delegation of basic auth credentials via federated SPARQL queries.
 */
public class FeatureCredentialDelegation extends IntegrationTest {

  private final MockHandler remote = new MockHandler();

  @Rule
  public HttpServer http =
      interactions.attached(Rules.httpServer("token-delegation-remote").with("/sparql", remote));

  private String federatedQuery() {
    return Pretty.format("SELECT * WHERE { SERVICE <%ssparql> { ?s ?p ?o }}", http.address());
  }

  // @formatter:off

  @Test
  public void ensure_sends_remote_request_on_mock_query() throws Exception {
    givenPermission("read")
      .formParam("query", federatedQuery())
    .when()
      .post("/sparql")
    .then();
      assertThat(remote.received(), is(notNullValue()));
  }

  @Test
  public void federated_queries_delegate_password() throws Exception {
    givenPermission("read")
      .header(delegateCredentialsHeader(), token("test-user", "test-password"))
      .formParam("query", federatedQuery())
    .when()
      .post("/sparql")
    .then();
      assertThat(remote.received(), basicAuthPassword("test-password"));
  }

  @Test
  public void username_is_dropped_in_delegated_credentials() throws Exception {
    givenPermission("read")
      .header(delegateCredentialsHeader(), token("test-user", "test-password"))
      .formParam("query", federatedQuery())
    .when()
      .post("/sparql")
    .then();
      assertThat(remote.received(), basicAuthUsername(""));
  }

  @Test
  public void handles_large_credential_payloads() throws Exception {
    givenPermission("read")
      .header(delegateCredentialsHeader(), token("test-user", Strings.repeat("test", 1000)))
      .formParam("query", federatedQuery())
    .when()
      .post("/sparql")
    .then();
      assertThat(remote.received(), basicAuthPassword(Strings.repeat("test", 1000)));
  }

  // @formatter:on

  /**
   * Encode username and password credentials for basic authentication
   *
   * @param username principal
   * @param password secret password
   * @return encoded header string
   */
  private String token(final String username, final String password) {
    return "Basic " + BaseEncoding.base64().encode(Payload.encodeUtf8(username + ":" + password));
  }

  /**
   * noop handler, that records exchanges
   */
  private static class MockHandler implements HttpHandler {
    public final AtomicReference<HttpExchange> received = new AtomicReference<>();

    public HttpExchange received() {
      final HttpExchange exchange = received.get();
      assert exchange != null : "no http exchange captures";
      return exchange;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
      final boolean wasNull = received.compareAndSet(null, exchange);
      assert wasNull : "multiple requests to mock server, lost " + exchange;
      exchange.sendResponseHeaders(500, -1);
      exchange.close();
    }
  }
}
