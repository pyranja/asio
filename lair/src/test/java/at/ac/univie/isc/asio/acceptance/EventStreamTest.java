package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.web.EventSource;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.http.HttpResponse;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.annotation.Nullable;
import javax.ws.rs.client.Entity;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
@Category(FunctionalTest.class)
public class EventStreamTest extends AcceptanceHarness {

  public static final Func1<EventSource.MessageEvent, String>
      EXTRACT_DATA = new Func1<EventSource.MessageEvent, String>() {
    @Override
    public String call(final EventSource.MessageEvent messageEvent) {
      return messageEvent.data();
    }
  };

  private EventSource monitor = EventSource.listenTo(adminAccess().resolve("meta/events"));

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sql");
  }

  @After
  public void tearDown() {
    monitor.close();
  }

  @Test
  public void emits_subscribed_event() throws Exception {
    final EventSource.MessageEvent received = monitor.events().take(1).toBlocking().single();
    assertThat(received.type(), is(equalToIgnoringCase("stream")));
    assertThat(received.data(), is("subscribed"));
  }

  @Test
  public void successful_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        client().request().async().post(Entity.entity("SELECT 1", Mime.QUERY_SQL.type()));
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(3).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, both(contains(message("received"), message("executed"), message("completed"))).and(is(correlated()))
    );
  }

  @Test
  public void failed_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        client().request().async().post(Entity.entity("SELECT * FROM NOT_EXISTING", Mime.QUERY_SQL.type()));
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(2).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, both(contains(message("received"), message("failed"))).and(is(correlated())));
  }

  @Test
  public void rejected_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        client().request().async().post(Entity.text("invalid"));
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(2).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, both(contains(message("received"), message("failed"))).and(is(correlated())));
  }

  private Matcher<String> message(final String message) {
    return containsString("\"message\":\"" + message + "\"");
  }

  private static String extractCorrelation(final String message) {
    final java.util.regex.Matcher regex =
        Pattern.compile(".*\"correlation\":\"(.+?)\".*").matcher(message);
    if (regex.matches() && regex.groupCount() == 1) {
      return regex.group(1);
    }
    throw new AssertionError("either no or too many correlations in " + message);
  }

  public static final CustomTypeSafeMatcher<Iterable<? extends String>> CORRELATION_MATCHER =
      new CustomTypeSafeMatcher<Iterable<? extends String>>("correlated") {
        @Override
        protected boolean matchesSafely(final Iterable<? extends String> item) {
          final Iterable<String> ids =
              Iterables.transform(item, new Function<String, String>() {
                @Nullable
                @Override
                public String apply(@Nullable final String input) {
                  return extractCorrelation(input);
                }
              });
          return everyItem(equalTo(Iterables.getFirst(ids, null))).matches(ids);
        }
      };

  private Matcher<Iterable<? extends String>> correlated() {
    return CORRELATION_MATCHER;
  }
}
