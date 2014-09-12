package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.EventSource;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import org.apache.http.HttpResponse;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.ws.rs.client.Entity;
import java.net.URI;
import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
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
    return readAccess().resolve("sql");
  }

  private EventSource monitor = EventSource.listenTo(adminAccess().resolve("events"));

  @After
  public void tearDown() {
    monitor.close();
  }

  @Test
  public void emits_subscribed_event() throws Exception {
    final EventSource.MessageEvent received = monitor.events().take(1).toBlocking().single();
    assertThat(received.type(), is(equalToIgnoringCase("system")));
    assertThat(received.data(), message("subscribed"));
  }

  private Matcher<String> message(final String message) {
    return containsString("\"message\":\""+ message +"\"");
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
        monitor.events().skip(1).take(4).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received,
        contains(message("received"), message("accepted"), message("executed"), message("completed")));
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
        monitor.events().skip(1).take(3).map(EXTRACT_DATA).toList().toBlocking().single();
    assertThat(received, contains(message("received"), message("accepted"), message("failed")));
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
    assertThat(received, contains(message("received"), message("rejected")));
  }
}
