package at.ac.univie.isc.asio.frontend;

import java.net.URI;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import at.ac.univie.isc.asio.DatasetEngine;

/**
 * Sets up a standalone servlet container that hosts an {@link EndpointApplication} with a mocked
 * {@link DatasetEngine}. The server is setup before the tests execute and destroyed after the last
 * finished. Before each test a {@link WebClient} is created and the engine mock is resetted.
 * 
 * @author Chris Borckholder
 */
public class EndpointTestFixture {

  private static final URI SERVER_URI = URI.create("http://localhost:1337/");

  // standalone servlet container setup

  private static Server server;
  protected static EndpointApplication application;

  @BeforeClass
  public static void initialize() {
    application = new EndpointApplication();
    final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
    final JAXRSServerFactoryBean factory =
        delegate.createEndpoint(application, JAXRSServerFactoryBean.class);
    // prefix application provided path with our fixed server base URL
    factory.setAddress(SERVER_URI.toString() + factory.getAddress());
    server = factory.create();
    server.start();
  }

  @AfterClass
  public static void destroy() {
    server.stop();
    server.destroy();
  }

  // test infrastructure

  protected WebClient client;
  protected EngineAdapter engine;

  @Before
  public void prepareClient() {
    client = WebClient.create(SERVER_URI);
    engine = application.getMockEngine();
  }

  @After
  public void resetClientAndMock() {
    client.reset();
    application.resetMocks();
  }
}
