package at.ac.univie.isc.asio.tool;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.junit.rules.ExternalResource;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Start an embedded CXF JAX-RS server hosting the given {@link Application}.
 *
 * @author Chris Borckholder
 */
public final class EmbeddedJaxrsServer extends ExternalResource {
  public static final URI HTTP_TRANSPORT_ADDRESS = URI.create("http://localhost:1337");
  public static final URI LOCAL_TRANSPORT_ADDRESS = URI.create("local://test"); // not with async!

  public static EmbeddedJaxrsServerBuilder host(final Application application) {
    return new EmbeddedJaxrsServerBuilder(application);
  }

  private final URI baseUri;
  private final JAXRSServerFactoryBean factory;

  private Server server;
  private URI serverUri;

  private EmbeddedJaxrsServer(final Application application, final URI baseUri,
                              final List<ResourceProvider> providers, final boolean shouldLog) {
    this.baseUri = requireNonNull(baseUri);
    requireNonNull(providers);
    requireNonNull(application);
    this.factory = initServerFactory(application, providers, shouldLog);
  }

  private JAXRSServerFactoryBean initServerFactory(final Application application,
      final List<ResourceProvider> providers, final boolean shouldLog) {
    final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
    final JAXRSServerFactoryBean factoryBean =
        delegate.createEndpoint(application, JAXRSServerFactoryBean.class);
    if (shouldLog) {
      factoryBean.getFeatures().add(new LoggingFeature());
    }
    factoryBean.setResourceProviders(providers);
    // prefix application provided path with our fixed server base URL
    serverUri = baseUri.resolve(factoryBean.getAddress());
    factoryBean.setAddress(serverUri.toString());
    return factoryBean;
  }

  @Override
  protected void before() throws Throwable {
    server = factory.create();
    server.start();
  }

  @Override
  protected void after() {
    server.stop();
    server.destroy();
  }

  /**
   * @return base URI of embedded server
   */
  @Deprecated
  public URI getBaseUri() {
    return baseUri;
  }

  /**
   * @return address of deployed JAXRS service
   */
  public URI getServerUri() {
    return serverUri;
  }

  public static class EmbeddedJaxrsServerBuilder {

    private final Application application;
    private URI baseUri = HTTP_TRANSPORT_ADDRESS;
    private boolean shouldLog = false;
    private List<ResourceProvider> providers = new ArrayList<>();

    EmbeddedJaxrsServerBuilder(final Application application) {
      this.application = application;
    }

    public EmbeddedJaxrsServerBuilder at(final URI baseUri) {
      this.baseUri = baseUri;
      return this;
    }

    public EmbeddedJaxrsServerBuilder resource(Object resource) {
      this.providers.add(FakePrototypeResourceProvider.of(resource));
      return this;
    }

    public EmbeddedJaxrsServerBuilder useLocalTransport() {
      this.baseUri = LOCAL_TRANSPORT_ADDRESS;
      return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmbeddedJaxrsServerBuilder enableLogging() {
      this.shouldLog = true;
      return this;
    }

    public EmbeddedJaxrsServer create() {
      return new EmbeddedJaxrsServer(application, baseUri, providers, shouldLog);
    }
  }


  /**
   * Simulate a prototype scoped JAXRS resource, but provide a singleton. Make sure,
   * the resource is not accessed from concurrent requests!
   */
  public static class FakePrototypeResourceProvider<T> implements ResourceProvider {
    public static <T> FakePrototypeResourceProvider<T> of(final T instance) {
      return new FakePrototypeResourceProvider<>(instance);
    }

    private final T instance;

    private FakePrototypeResourceProvider(final T instance) {
      this.instance = requireNonNull(instance);
    }

    @Override
    public Object getInstance(final Message m) {
      return instance;
    }

    @Override
    public void releaseInstance(final Message m, final Object o) {
      /* noop */
    }

    @Override
    public Class<?> getResourceClass() {
      return instance.getClass();
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
  }
}
