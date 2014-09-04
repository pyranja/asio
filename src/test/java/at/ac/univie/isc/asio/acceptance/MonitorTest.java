package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.EventSource;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import at.ac.univie.isc.asio.tool.Rules;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class MonitorTest extends AcceptanceHarness {

  public static final Func1<EventSource.MessageEvent, String>
      EXTRACT_DATA = new Func1<EventSource.MessageEvent, String>() {
    @Override
    public String call(final EventSource.MessageEvent messageEvent) {
      return messageEvent.data();
    }
  };

  @Override
  protected URI getTargetUrl() {
    return READ_ACCESS.resolve("sql");
  }

  @Rule
  public Timeout timeout = Rules.timeout(4, TimeUnit.SECONDS);

  private EventSource monitor = EventSource.listenTo(ADMIN_ACCESS.resolve("events"));

  @After
  public void tearDown() {
    monitor.close();
  }

  @Test
  public void emits_subscribed_event() throws Exception {
    final EventSource.MessageEvent received = monitor.events().take(1).toBlocking().single();
    assertThat(received.type(), is("system"));
    assertThat(received.data(), message("subscribed"));
  }

  private Matcher<String> message(final String message) {
    return containsString("\"message\":\""+ message +"\"");
  }

  @Test
  public void successful_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<Void>() {
      @Override
      public void call(final Void aVoid) {
        client.accept(XML).query(PARAM_QUERY, "SELECT 1").async().get();
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(4).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received,
        contains(message("received"), message("accepted"), message("executed"), message("completed")));
  }

  @Test
  public void failed_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<Void>() {
      @Override
      public void call(final Void aVoid) {
        client.accept(XML).query(PARAM_QUERY, "SELECT * FROM NOT_EXISTING").async().get();
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(3).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, contains(message("received"), message("accepted"), message("failed")));
  }

  @Test
  public void rejected_query_event_sequence() throws Exception {
    monitor.connection().subscribe(new Action1<Void>() {
      @Override
      public void call(final Void aVoid) {
        client.accept(XML).async().get();
      }
    });
    final List<String> received =
        monitor.events().skip(1).take(2).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, contains(message("received"), message("rejected")));
  }
}
