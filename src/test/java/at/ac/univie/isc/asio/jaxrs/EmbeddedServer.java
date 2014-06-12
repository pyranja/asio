package at.ac.univie.isc.asio.jaxrs;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Start an embedded CXF JAX-RS server hosting the given {@link javax.ws.rs.core.Application}.
 *
 * @author Chris Borckholder
 */
public final class EmbeddedServer extends ExternalResource {
  public static final URI HTTP_TRANSPORT_ADDRESS = URI.create("http://localhost:1337");
  public static final URI LOCAL_TRANSPORT_ADDRESS = URI.create("local://test"); // not with async!

  public static EmbeddedServerBuilder host(final Application application) {
    return new EmbeddedServerBuilder(application);
  }

  private final URI baseUri;
  private final JAXRSServerFactoryBean factory;
  private final ManagedClient client;

  private Server server;
  private URI serviceAddress;

  private EmbeddedServer(final Application application, final ManagedClient.ManagedClientBuilder clientConfig, final URI baseUri,
                         final List<ResourceProvider> providers, final boolean shouldLog) {
    this.baseUri = requireNonNull(baseUri);
    requireNonNull(providers);
    requireNonNull(application);
    this.factory = initServerFactory(application, providers, shouldLog);
    client = clientConfig.build(serviceAddress);
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
    serviceAddress = baseUri.resolve(factoryBean.getAddress());
    factoryBean.setAddress(serviceAddress.toString());
    return factoryBean;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    final Statement inner = client.apply(base, description);
    return super.apply(inner, description);
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
   * @return address of deployed JAXRS service
   */
  public URI getServiceAddress() {
    return serviceAddress;
  }

  /**
   * @return managed client instance targeting this service's address
   */
  public WebTarget endpoint() {
    return client.endpoint();
  }

  public static class EmbeddedServerBuilder {

    private final Application application;
    private URI baseUri = HTTP_TRANSPORT_ADDRESS;
    private boolean shouldLog = false;
    private List<ResourceProvider> providers = new ArrayList<>();
    private ManagedClient.ManagedClientBuilder client = ManagedClient.create();

    EmbeddedServerBuilder(final Application application) {
      this.application = application;
    }

    public EmbeddedServerBuilder at(final URI baseUri) {
      this.baseUri = baseUri;
      return this;
    }

    public EmbeddedServerBuilder resource(Object resource) {
      this.providers.add(FakePrototypeResourceProvider.of(resource));
      return this;
    }

    public EmbeddedServerBuilder useLocalTransport() {
      this.baseUri = LOCAL_TRANSPORT_ADDRESS;
      return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmbeddedServerBuilder enableLogging() {
      this.shouldLog = true;
      return this;
    }

    public EmbeddedServerBuilder clientConfig(final ManagedClient.ManagedClientBuilder builder) {
      this.client = builder;
      return this;
    }

    public EmbeddedServer create() {
      return new EmbeddedServer(application, client, baseUri, providers, shouldLog);
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
