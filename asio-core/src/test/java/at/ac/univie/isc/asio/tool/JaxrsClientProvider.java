package at.ac.univie.isc.asio.tool;

import java.net.URI;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.rules.ExternalResource;

import com.google.common.base.Supplier;

/**
 * Sets up a cxf WebClient before each test and disposes it after the test. If a test fails, the
 * contents of the last response received are logged.
 * 
 * @author Chris Borckholder
 */
public final class JaxrsClientProvider extends ExternalResource implements Supplier<WebClient> {

  private final URI baseAddress;
  private WebClient client;

  public JaxrsClientProvider(final URI baseAddress) {
    super();
    this.baseAddress = baseAddress;
  }

  @Override
  public WebClient get() {
    assert client != null : "JAX-RS client not initialized";
    return client;
  }

  @Override
  protected void before() throws Throwable {
    client = WebClient.create(baseAddress);
  }

  @Override
  protected void after() {
    client.close();
  }
}
