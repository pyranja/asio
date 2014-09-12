package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.metadata.StaticMetadata;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.MediaType;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class MetadataTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("meta");
  }

  @Test
  public void should_deliver_metadata_xml_document() throws Exception {
    response = client().request(Mime.XML.type()).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(Mime.XML.type().isCompatible(response.getMediaType()), is(true));
    final DatasetMetadata document = response.readEntity(DatasetMetadata.class);
    assertThat(document, is(equalTo(StaticMetadata.NOT_AVAILABLE)));
  }

  @Test
  public void should_deliver_metadata_json_document() throws Exception {
    response = client().request(Mime.JSON.type()).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), is(true));
    final DatasetMetadata document = response.readEntity(DatasetMetadata.class);
    assertThat(document, is(equalTo(StaticMetadata.NOT_AVAILABLE)));
  }
}
