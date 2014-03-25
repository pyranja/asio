package at.ac.univie.isc.asio.protocol;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class ContentTypeRegexTest {

  private final Pattern p = Endpoint.MEDIA_SUBTYPE_PATTERN;

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
