package at.ac.univie.isc.asio.tool;

import java.net.URI;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.junit.rules.ExternalResource;

/**
 * Start an embedded CXF JAX-RS server hosting the given {@link Application}.
 * 
 * @author Chris Borckholder
 */
public class EmbeddedJaxrsServer extends ExternalResource {

  private final URI baseUri;
  private final Application application;

  private Server server;
  private boolean shouldLog = false;

  /**
   * @param application configuration of JAX-RS app
   * @param baseUri publish URL of server
   */
  public EmbeddedJaxrsServer(final Application application, final URI baseUri) {
    super();
    this.baseUri = baseUri;
    this.application = application;
  }

  /**
   * Uses a default base url
   * 
   * @param application to be hosted
   */
  public EmbeddedJaxrsServer(final Application application) {
    this(application, URI.create("http://localhost:1337"));
  }

  public EmbeddedJaxrsServer enableLogging() {
    shouldLog = true;
    return this;
  }

  @Override
  protected void before() throws Throwable {
    final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
    final JAXRSServerFactoryBean factory =
        delegate.createEndpoint(application, JAXRSServerFactoryBean.class);
    if (shouldLog) {
      factory.getFeatures().add(new LoggingFeature());
    }
    // prefix application provided path with our fixed server base URL
    factory.setAddress(baseUri.toString() + factory.getAddress());
    server = factory.create();
    server.start();
  }

  @Override
  protected void after() {
    server.stop();
    server.destroy();
  }

  /**
   * @return entry point of the hosted application
   */
  public URI getBaseUri() {
    return baseUri;
  }
}
