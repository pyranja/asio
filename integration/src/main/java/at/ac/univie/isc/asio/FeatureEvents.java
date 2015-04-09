package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.insight.EventStream;
import at.ac.univie.isc.asio.integration.IntegrationTest;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static at.ac.univie.isc.asio.insight.Events.payload;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Emitting events from protocol operations.
 */
@SuppressWarnings("unchecked")
@Category(Integration.class)
@RunWith(Parameterized.class)
public class FeatureEvents extends IntegrationTest {

  @Parameterized.Parameters(name = "{index} : {0}-{1}")
  public static Iterable<Object[]> variants() {
    // { language, operation, noop_command }
    return Arrays.asList(new Object[][] {
        {"sql", "query", "SELECT 1"},
        {"sql", "update", "DROP TABLE IF EXISTS test_gaga_12345"},
        {"sparql", "query", "ASK {}"},
    });
  }

  @Parameterized.Parameter(0)
  public String language;
  @Parameterized.Parameter(1)
  public String operation;
  @Parameterized.Parameter(2)
  public String noop;

  @Before
  public void ensureLanguageSupported() {
    ensureLanguageSupported(language);
  }

  @Test
  public void emit_subscribed_on_connecting() throws Exception {
    final InboundEvent received = eventStream().take(1).toBlocking().single();
    assertThat(received, both(payload(containsString("\"type\":\"stream\"")))
        .and(payload(containsString("\"subject\":\"subscribed\""))));
  }

  @Test
  public void successful_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().skip(1).take(3));
    given().role("admin").and().param(operation, noop).post("/{language}", language)
        .then().statusCode(is(HttpStatus.SC_OK));
    assertThat(received, both(sequence("received", "executed", "completed")).and(correlated()));
  }

  @Test
  public void failed_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().skip(1).take(3));
    final String invalidCommand =
        Hashing.md5().hashString(noop, Charsets.UTF_8).toString();
    given().role("admin").and().param(operation, invalidCommand).post("/{language}", language);
    assertThat(received, both(sequence("received", "failed", "error")).and(correlated()));
  }

  @Test
  public void rejected_query_event_sequence() throws Exception {
    final Iterable<InboundEvent> received =
        EventStream.collectAll(eventStream().skip(1).take(3));
    given().role("admin").and().param(operation).post("/{language}", language);
    assertThat(received, both(sequence("received", "failed", "error")).and(correlated()));
  }

  private Matcher<Iterable<InboundEvent>> sequence(final String... subjects) {
    Matcher<? super InboundEvent>[] subjectMatchers = new Matcher[subjects.length];
    for (int i = 0; i < subjects.length; i++) {
      subjectMatchers[i] = payload(containsString("\"subject\":\"" + subjects[i] + "\""));
    }
    return Matchers.hasItems(subjectMatchers);
  }

  private Matcher<Iterable<? extends InboundEvent>> correlated() {
    return new CorrelationMatcher();
  }

  private static class CorrelationMatcher extends TypeSafeMatcher<Iterable<? extends InboundEvent>> {
    @Override
    protected boolean matchesSafely(final Iterable<? extends InboundEvent> item) {
      final Iterable<String> ids =
          Iterables.transform(item, new Function<InboundEvent, String>() {
            @Override
            public String apply(final InboundEvent input) {
              return extractCorrelation(input.readData());
            }
          });
      return everyItem(equalTo(Iterables.getFirst(ids, null))).matches(ids);
    }

    private String extractCorrelation(final String message) {
      final java.util.regex.Matcher regex =
          Pattern.compile(".*\"correlation\":\"(.+?)\".*").matcher(message);
      if (regex.matches() && regex.groupCount() == 1) {
        return regex.group(1);
      }
      throw new AssertionError("either no or too many correlations in " + message);
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("correlated event sequence");
    }
  }
}
