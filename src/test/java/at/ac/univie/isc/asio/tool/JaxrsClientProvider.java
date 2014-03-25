package at.ac.univie.isc.asio.tool;

import com.google.common.base.Supplier;

import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.rules.ExternalResource;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sets up a cxf WebClient before each test and disposes it after the test. If a test fails, the
 * contents of the last response received are logged.
 * 
 * @author Chris Borckholder
 */
public final class JaxrsClientProvider extends ExternalResource implements Supplier<WebClient> {

  private final URI baseAddress;
  private final List<Object> providers;
  private WebClient client;

  public JaxrsClientProvider(final URI baseAddress) {
    super();
    this.baseAddress = baseAddress;
    this.providers = new CopyOnWriteArrayList<>();
  }

  @Override
  public WebClient get() {
    assert client != null : "JAX-RS client not initialized";
    return client;
  }

  public JaxrsClientProvider with(Object provider) {
    providers.add(provider);
    return this;
  }

  @Override
  protected void before() throws Throwable {
    JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
    factory.setAddress(baseAddress.toString());
    factory.setProviders(providers);
    client = factory.createWebClient();
  }

  @Override
  protected void after() {
    client.close();
  }
}
