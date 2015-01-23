package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.tool.ValueOrError;
import org.junit.Test;
import org.mockito.internal.matchers.Not;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ExtractOperationTest {
  private final ExtractOperation subject = ExtractOperation.expect(Language.valueOf("language"));
  private ValueOrError<String> extracted;

  @Test
  public void should_parse_valid_operation() throws Exception {
    extracted = subject.from(MediaType.valueOf("*/language-operation"));
    assertThat(extracted.get(), is("operation"));
  }

  @Test
  public void should_fail_on_null_mime() throws Exception {
    extracted = subject.from(null);
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  @Test
  public void should_fail_on_language_mismatch() throws Exception {
    extracted = subject.from(MediaType.valueOf("*/illegal-operation"));
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  @Test
  public void should_fail_on_malformed_mime() throws Exception {
    extracted = subject.from(MediaType.WILDCARD_TYPE);
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  private final Pattern p = ExtractOperation.MEDIA_SUBTYPE_PATTERN;

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
