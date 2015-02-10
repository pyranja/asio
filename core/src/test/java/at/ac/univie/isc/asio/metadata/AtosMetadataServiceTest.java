package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.io.Classpath;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteSource;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.common.net.PercentEscaper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// @formatter:off+
public class AtosMetadataServiceTest {

  private static final PercentEscaper URL_ENCODER = new PercentEscaper("/_.*", false);
  private static final String XML = MediaType.APPLICATION_XML_UTF_8.withoutParameters().toString();

  @ClassRule
  public static WireMockRule SERVER = new WireMockRule(7042);

  public static ByteSource EXPECTED_METADATA = Classpath.load("metadata/ref_wp4_expected.xml");
public static ByteSource VALID_ANSWER = Classpath.load("metadata/ref_wp4_localid_answer.xml");
public static ByteSource EMPTY_ANSWER = Classpath.load("metadata/ref_wp4_empty_answer.xml");

  private static final URI REPOSITORY = URI.create("http://localhost:7042/atos");
  private static final String LOCAL_ID = "https://vphsharedata1.sheffield.ac.uk/richtest2";
  private static final MappingBuilder VALID_REQUEST =
      get(urlMatching("/atos/metadata/facets/dataset/localID\\?value=.+"))
          .withHeader(HttpHeaders.ACCEPT, equalTo(XML));
  private static final MappingBuilder ANY_REQUEST = any(urlMatching(".*"));

  @Rule
  public final ExpectedException fail = ExpectedException.none();

  private final AtosMetadataService subject = new AtosMetadataService(REPOSITORY);

  @Test
  public void should_retrieve_existing_dataset_metadata() throws Exception {
    givenThat(VALID_REQUEST
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader(HttpHeaders.CONTENT_TYPE, XML)
        .withBody(VALID_ANSWER.read())
       ));
    final DatasetMetadata result = subject.fetchMetadataForId(LOCAL_ID);
    assertThat(result, is(expected_metadata()));
  }

  @Test
  public void should_use_correct_query_param_and_accept_header() throws Exception {
    try {
      subject.fetchMetadataForId(LOCAL_ID);
    } catch (final Exception ignored) {}
    final String escaped = URL_ENCODER.escape(LOCAL_ID);
    verify(getRequestedFor(urlEqualTo("/atos/metadata/facets/dataset/localID?value="+ escaped))
      .withHeader(HttpHeaders.ACCEPT, equalTo(XML)));
  }

  @Test
  public void should_propagate_repository_failure() throws Exception {
    givenThat(ANY_REQUEST.willReturn(aResponse().withStatus(500)));
    fail.expect(RepositoryFailure.class);
    fail.expectMessage("500");
    subject.fetchMetadataForId(LOCAL_ID);
  }

  @Test
  public void should_fail_on_malformed_answer() throws Exception {
    givenThat(ANY_REQUEST.willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    fail.expect(RepositoryFailure.class);
    subject.fetchMetadataForId(LOCAL_ID);
  }

  @Test
  public void should_fail_on_unexpected_payload() throws Exception {
    givenThat(ANY_REQUEST.willReturn(aResponse()
      .withStatus(200).withBody("<payload>unexpected</payload>")
      ));
    fail.expect(MetadataNotFound.class);
    subject.fetchMetadataForId(LOCAL_ID);
  }

  @Test
  public void should_fail_on_malformed_payload() throws Exception {
    givenThat(ANY_REQUEST.willReturn(aResponse()
      .withStatus(200)
      .withBody("<resource_metadata_list><resource_metadata>malformed</resource_metadata></resource_metadata_list>")
      ));
    fail.expect(MetadataNotFound.class);
    subject.fetchMetadataForId(LOCAL_ID);
  }

  @Test
  public void should_fail_on_empty_result() throws Exception {
    givenThat(VALID_REQUEST.willReturn(aResponse()
      .withStatus(200).withBody(EMPTY_ANSWER.read())
      ));
    fail.expect(MetadataNotFound.class); // should differ malformed response and empty list response
    subject.fetchMetadataForId(LOCAL_ID);
  }

  private DatasetMetadata expected_metadata() throws IOException {
    try (InputStream stream = EXPECTED_METADATA.openStream()) {
      return JAXB.unmarshal(stream, DatasetMetadata.class);
    }
  }
}
