package at.ac.univie.isc.asio.acceptance;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.metadata.StaticMetadata;
import at.ac.univie.isc.asio.tool.FunctionalTest;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 3/20/14 ; Time: 12:25 PM
 */
@Category(FunctionalTest.class)
public class MetadataTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("meta");
  }

  @Test
  public void should_deliver_metadata_xml_document() throws Exception {
    response = client.accept(XML).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(XML.isCompatible(response.getMediaType()), is(true));
    final DatasetMetadata document = response.readEntity(DatasetMetadata.class);
    assertThat(document, is(equalTo(StaticMetadata.NOT_AVAILABLE)));
  }

  @Test
  public void should_deliver_metadata_json_document() throws Exception {
    response = client.accept(MediaType.APPLICATION_JSON_TYPE).get();
    assertThat(familyOf(response.getStatus()), is(SUCCESSFUL));
    assertThat(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), is(true));
    final DatasetMetadata document = response.readEntity(DatasetMetadata.class);
    assertThat(document, is(equalTo(StaticMetadata.NOT_AVAILABLE)));
  }
}
