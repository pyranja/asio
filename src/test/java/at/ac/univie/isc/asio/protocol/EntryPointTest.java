package at.ac.univie.isc.asio.protocol;

import com.google.common.collect.ImmutableSet;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.frontend.DatasetExceptionMapper;
import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.metadata.MockMetadata;
import at.ac.univie.isc.asio.tool.EmbeddedJaxrsServer;
import at.ac.univie.isc.asio.tool.JaxrsClientProvider;
import at.ac.univie.isc.asio.tool.ResponseMonitor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EntryPointTest {

  @ApplicationPath("/")
  private static class EntryPointApplication extends Application {

    private final EndpointSupplier supplier = Mockito.mock(EndpointSupplier.class);

    @Override
    public Set<Object> getSingletons() {
      return ImmutableSet.of(new EntryPoint(supplier), new DatasetExceptionMapper());
    }
  }

  public static final EntryPointApplication application = new EntryPointApplication();
  @ClassRule
  public static EmbeddedJaxrsServer server = new EmbeddedJaxrsServer(application).enableLogging();
  public JaxrsClientProvider provider = new JaxrsClientProvider(server.getBaseUri());
  @Rule
  public TestRule chain = RuleChain.outerRule(provider).around(new ResponseMonitor(provider));

  private WebClient client;

  @Before
  public void setUp() {
    client = provider.get();
  }

  @Test
  public void should_request_endpoint_for_language_in_uri() throws Exception {
    client.path("/read/sql");
    client.get();
    Mockito.verify(application.supplier).get(Language.SQL);
  }

  @Test
  public void should_reject_unknown_permission() throws Exception {
    client.path("/unknown/sql");
    final Response response = client.get();
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
    Mockito.verifyZeroInteractions(application.supplier);
  }

  @Test
  public void should_reject_unknown_language() throws Exception {
    client.path("/read/unknown");
    final Response response = client.get();
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
    Mockito.verifyZeroInteractions(application.supplier);
  }
  
  // meta data
  @Test
  public void should_serve_metadata_document() throws Exception {
    client.path("/read/meta");
    final Response response = client.get();
    assertThat(response.getStatusInfo().getFamily(), is(Family.SUCCESSFUL));
  }

  @Test
  public void should_provide_mock_metadata() throws Exception {
    client.path("/read/meta");
    final DatasetMetadata metadata = client.get(DatasetMetadata.class);
    assertThat(metadata, is(equalTo(MockMetadata.INSTANCE)));
  }
}
