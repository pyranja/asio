package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Language;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.ac.univie.isc.asio.tool.IsMultimapContaining.hasEntries;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParametersTest {
  private final HttpHeaders headers = mock(HttpHeaders.class);

  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Parameters.ParametersBuilder builder =
      Parameters.builder(Language.TEST);
  private Parameters subject;

  @Before
  public void setup() {
    when(headers.getAcceptableMediaTypes()).thenReturn(Collections.<MediaType>emptyList());
    when(headers.getRequestHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
  }

  @Test
  public void should_set_language_param() throws Exception {
    subject = builder.build(headers);
    assertThat(subject.language(), is(Language.TEST));
  }

  @Test
  public void should_add_all_params_from_a_map() throws Exception {
    final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    map.putSingle("single", "value");
    map.put("multiple", Arrays.asList("one", "two"));
    subject = builder.add(map).build(headers);
    assertThat(subject.properties(), hasEntries("multiple", "one", "two"));
    assertThat(subject.require("single"), is("value"));
  }

  @Test
  public void should_add_body_param() throws Exception {
    subject = builder.body("command", MediaType.valueOf("application/test-query")).build(headers);
    assertThat(subject.require("query"), is("command"));
  }

  @Test
  public void should_fail_on_language_mismatch() throws Exception {
    subject = builder.body("command", MediaType.valueOf("application/sql-query")).build(headers);
    error.expect(NotSupportedException.class);
    subject.properties();
  }

  @Test
  public void should_fail_on_malformed_content_type() throws Exception {
    subject = builder.body("command", MediaType.valueOf("application/json")).build(headers);
    error.expect(NotSupportedException.class);
    subject.properties();
  }

  @Test
  public void should_fail_on_null_body() throws Exception {
    subject = builder.body(null, MediaType.valueOf("application/test-update")).build(headers);
    error.expect(BadRequestException.class);
    subject.properties();
  }

  @Test
  public void should_fail_on_null_headers() throws Exception {
    subject = builder.build(null);
    error.expect(NullPointerException.class);
    subject.properties();
  }


  @Test
  public void should_add_manual_accepted_type() throws Exception {
    subject = builder.accept(MediaType.TEXT_PLAIN_TYPE).build(headers);
    assertThat(subject.acceptable(), is(Arrays.asList(MediaType.TEXT_PLAIN_TYPE)));
  }

  @Test
  public void should_add_accepted_from_headers() throws Exception {
    when(headers.getAcceptableMediaTypes())
        .thenReturn(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.TEXT_XML_TYPE));
    subject = builder.build(headers);
    assertThat(subject.acceptable(),
        containsInAnyOrder(MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.TEXT_XML_TYPE));
  }

  @Test
  public void should_yield_accepted_from_manual_and_header() throws Exception {
    when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM_TYPE));
    subject = builder.accept(MediaType.APPLICATION_XML_TYPE).build(headers);
    assertThat(subject.acceptable(),
        containsInAnyOrder(MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_XML_TYPE));
  }

  // ======= MEDIATYPE REGEX TESTS =======

  private final Pattern p = Parameters.ParametersBuilder.MEDIA_SUBTYPE_PATTERN;

  @Test
  public void should_find_language() throws Exception {
    final String[] parsed = apply("TEST-QUERY");
    assertThat(parsed[0], is("TEST"));
  }

  @Test
  public void should_identify_query() throws Exception {
    final String[] parsed = apply("TEST-QUERY");
    assertThat(parsed[1], is("QUERY"));
  }

  @Test
  public void should_identify_update() throws Exception {
    final String[] parsed = apply("TEST-UPDATE");
    assertThat(parsed[1], is("UPDATE"));
  }

  private String[] apply(final String input) {
    final Matcher match = p.matcher(input);
    assertThat(match.matches(), is(true));
    assertThat(match.groupCount(), is(2));
    return new String[] {match.group(1), match.group(2)};
  }
}
