package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.config.JaxrsSpec;
import at.ac.univie.isc.asio.jaxrs.EmbeddedServer;
import com.google.common.base.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataResourceTest {
  private final Supplier<DatasetMetadata> metaSource = mock(Supplier.class);

  @Rule
  public Timeout timeout = new Timeout(2000);
  @Rule
  public EmbeddedServer service = EmbeddedServer
      .host(JaxrsSpec.create(MetadataResource.class))
      .resource(new MetadataResource(metaSource))
      .enableLogging()
      .create();

  private WebTarget endpoint;

  @Before
  public void setup() {
    endpoint = service.endpoint().path("{permission}").path("meta");
    when(metaSource.get()).thenReturn(StaticMetadata.MOCK_METADATA);
  }

  @Test
  public void should_fetch_xml_metadata() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_fetch_json_metadata() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").request(MediaType.APPLICATION_JSON).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_not_allow_access_for_unauthorized() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "none").request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasStatus(Response.Status.FORBIDDEN));
  }
}
