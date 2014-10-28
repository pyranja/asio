package at.ac.univie.isc.asio.jaxrs;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

/**
 * Created by pyranja on 04/05/2014.
 */
@RunWith(MockitoJUnitRunner.class)
public class ContentNegotiationDefaultsFilterTest {

  public static final Matcher<Map<? extends String, ? extends List<String>>>
      ACCEPTS_DEFAULT_TYPE = hasEntry(HttpHeaders.ACCEPT, asList(MediaType.APPLICATION_XML));
  public static final Matcher<Map<? extends String, ? extends List<String>>>
      ACCEPTS_DEFAULT_LANGUAGE = hasEntry(HttpHeaders.ACCEPT_LANGUAGE,
      asList(Locale.ENGLISH.getLanguage()));

  private ContentNegotiationDefaultsFilter subject;

  @Mock
  private ContainerRequestContext context;
  private MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

  @Before
  public void setup() {
    subject = new ContentNegotiationDefaultsFilter();
    Mockito.when(context.getHeaders()).thenReturn(headers);
  }

  @Test
  public void should_set_accept_header_if_missing() throws Exception {
    subject.filter(context);
    assertThat(headers, ACCEPTS_DEFAULT_TYPE);
  }

  @Test
  public void should_not_replace_existing_accept_header() throws Exception {
    headers.putSingle(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);
    subject.filter(context);
    assertThat(headers, hasEntry(HttpHeaders.ACCEPT, asList(MediaType.TEXT_PLAIN)));
  }

  @Test
  public void should_set_accept_header_if_present_but_empty() throws Exception {
    headers.put(HttpHeaders.ACCEPT, Lists.<String>newArrayList());
    subject.filter(context);
    assertThat(headers, ACCEPTS_DEFAULT_TYPE);
  }

  @Test
  public void should_replace_wildcard_with_default_type() throws Exception {
    headers.putSingle(HttpHeaders.ACCEPT, MediaType.WILDCARD);
    subject.filter(context);
    assertThat(headers, ACCEPTS_DEFAULT_TYPE);
  }

  @Test
  public void should_not_replace_wildcard_if_concrete_type_present() throws Exception {
    headers.addAll(HttpHeaders.ACCEPT, MediaType.WILDCARD, MediaType.TEXT_PLAIN);
    subject.filter(context);
    final List<String> accepted = headers.get(HttpHeaders.ACCEPT);
    assertThat(accepted, containsInAnyOrder(MediaType.WILDCARD, MediaType.TEXT_PLAIN));
  }

  @Test
  public void should_set_default_language_if_missing() throws Exception {
    subject.filter(context);
    assertThat(headers, ACCEPTS_DEFAULT_LANGUAGE);
  }

  @Test
  public void should_not_replace_if_language_tag_present() throws Exception {
    headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "fr");
    subject.filter(context);
    assertThat(headers, hasEntry(HttpHeaders.ACCEPT_LANGUAGE, asList("fr")));
  }
}
