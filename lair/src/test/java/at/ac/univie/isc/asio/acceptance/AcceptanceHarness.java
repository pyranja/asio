package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.ManagedClient;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.junit.Rules;
import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;


public abstract class AcceptanceHarness {
  private ManagedClient provider;

  protected Response response;

  @Rule
  public final Timeout timeout = Rules.timeout(TestContext.timeout(), TimeUnit.SECONDS);
  @Rule
  public final Interactions interactions = Interactions.empty();

  @Rule
  public final TestRule clientProvider() {
    final KeyStore keyStore = TestContext.keyStore();
    JSONProvider jsonSerializer = new JSONProvider();
    jsonSerializer
        .setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio", "asio"));
    provider = ManagedClient.create()
        .use(jsonSerializer)
        .secured(keyStore)
        .build(getTargetUrl());
    interactions.and(provider);
    return provider;
  }

  protected abstract URI getTargetUrl();

  protected final WebTarget client() {
    return provider.endpoint();
  }

  protected final WebTarget client(final URI address) {
    return provider.target(address);
  }

  protected final Database database() {
    return TestContext.database();
  }

  protected final URI serverAddress() {
    return TestContext.serverAddress();
  }

  protected final URI readAccess() {
    return serverAddress().resolve("read/");
  }

  protected final URI fullAccess() {
    return serverAddress().resolve("full/");
  }

  protected final URI adminAccess() {
    return serverAddress().resolve("admin/");
  }
}
