package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.jaxrs.EventSource;
import at.ac.univie.isc.asio.tool.Duration;
import at.ac.univie.isc.asio.tool.EmbeddedTomcat;
import at.ac.univie.isc.asio.tool.Rules;
import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.net.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.schedulers.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.*;
import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.assertThat;

public class EventMonitorServletTest {
  public static final EventSource.MessageEvent.Builder TEST_EVENT =
      EventSource.MessageEvent.create().withType("TEST");

  private final EventBus events = new EventBus();
  private final ExecutorService exec = Executors.newCachedThreadPool();
  private final EventMonitorServlet subject = new EventMonitorServlet(
      Schedulers.from(exec)
      , new Function<ServletContext, EventBus>() {
    @Nonnull
    @Override
    public EventBus apply(@Nullable final ServletContext input) {
      return events;
    }
  }
      , Duration.create(50L, TimeUnit.MILLISECONDS)
  );

  @Rule
  public Timeout timeout = Rules.timeout(2, TimeUnit.SECONDS);
  @Rule
  public EmbeddedTomcat tomcat = EmbeddedTomcat.with("monitor", subject);

  private EventSource monitor;

  @Before
  public void setUp() {
    monitor = EventSource.listenTo(getServiceAddress(), Schedulers.newThread());
  }

  @After
  public void tearDown() throws InterruptedException {
    monitor.close();
    exec.shutdownNow();
    exec.awaitTermination(500, TimeUnit.MILLISECONDS);
  }

  @Test
  @Ignore("for manual testing")
  public void manual_run() throws Exception {
    for (int i = 0; i < 1000; i++) {
      subject.publish(event(i + ""));
      Thread.sleep(200);
    }
  }

  @Test
  public void head_does_not_block() throws Exception {
    final Response response = tomcat.endpoint().path("monitor").request().head();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(MediaType.valueOf("text/event-stream"))));
    assertThat(response,
        both(hasHeader(equalToIgnoringCase(HttpHeaders.CACHE_CONTROL),
            allOf(containsString("no-cache"), containsString("no-store"), containsString("max-age=0"), containsString("must-revalidate"))))
            .and(hasHeader(HttpHeaders.PRAGMA, "no-cache"))
            .and(hasHeader(HttpHeaders.CONNECTION, "keep-alive")));
  }

  @Test
  public void valid_event_stream_metadata() throws Exception {
    final AtomicReference<HttpResponse> responseHolder = new AtomicReference<>();
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse httpResponse) {
        responseHolder.set(httpResponse);
      }
    });
    monitor.events().subscribe();
    await().untilAtomic(responseHolder, is(notNullValue()));
    final HttpResponse response = responseHolder.get();
    assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.SC_OK));
    final MediaType responseFormat = MediaType.valueOf(response.getEntity().getContentType().getValue());
    assertThat(responseFormat, is(compatibleTo(MediaType.valueOf("text/event-stream"))));
    assertThat(response.getFirstHeader(HttpHeaders.CACHE_CONTROL).getValue()
      , allOf(containsString("no-cache"), containsString("no-store"), containsString("max-age=0"), containsString("must-revalidate"))
    );
    assertThat(response.getFirstHeader(HttpHeaders.PRAGMA).getValue(), is("no-cache"));
    assertThat(response.getFirstHeader(HttpHeaders.CONNECTION).getValue(), is("keep-alive"));
  }

  private final StringWriter sink = new StringWriter();

  @Test
  public void writes_typed_event() throws Exception {
    final ServerSentEvent event =
        ServerSentEvent.Default.create(ServerSentEvent.Type.valueOf("test-type"), "test-payload");
    subject.writeEvent(event, sink);
    assertThat(sink.toString(), is(equalToIgnoringCase("event:test-type\ndata:test-payload\n\n")));
  }

  @Test
  public void writes_generic_event() throws Exception {
    final ServerSentEvent event =
        ServerSentEvent.Default.create(ServerSentEvent.GENERIC, "test-payload");
    subject.writeEvent(event, sink);
    assertThat(sink.toString(), is("data:test-payload\n\n"));
  }

  @Test
  public void writes_comment() throws Exception {
    final ServerSentEvent event =
        ServerSentEvent.Default.create(ServerSentEvent.COMMENT, "test-payload");
    subject.writeEvent(event, sink);
    assertThat(sink.toString(), is(":test-payload\n"));
  }

  @Test
  @Ignore("belongs to event source tests")
  public void comment_typed_events_are_ignored_by_event_source() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        events.post(ServerSentEvent.Default.create(ServerSentEvent.COMMENT, "test"));
        subject.destroy();
      }
    });
    final List<EventSource.MessageEvent> received = monitor
        .events().skip(1).toList().toBlocking().single();
    assertThat(received, is(empty()));
  }

  @Test
  public void initial_event_is_subscription_confirmation() throws Exception {
    final EventSource.MessageEvent event = monitor.events().take(1).toBlocking().single();
    assertThat(event.type(), is(equalToIgnoringCase("system")));
    assertThat(event.data(), is("{\"message\":\"subscribed\"}"));
  }

  @Test
  public void publishes_events_in_order() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse ignore) {
        subject.publish(event("1"));
        subject.publish(event("2"));
      }
    });
    final Iterable<EventSource.MessageEvent> received =
        monitor.events().skip(1).take(2).toList().toBlocking().single();
    assertThat(received, contains(
        TEST_EVENT.withData("1")
        , TEST_EVENT.withData("2")
    ));
  }

  @Test
  public void multicast_events() throws Exception {
    final EventSource first = EventSource.listenTo(getServiceAddress(), Schedulers.newThread());
    final EventSource second = EventSource.listenTo(getServiceAddress(), Schedulers.newThread());
    second.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse ignored) {
        subject.publish(event("1"));
        subject.publish(event("2"));
      }
    });
    final Future<List<EventSource.MessageEvent>> firstFuture =
        first.events().take(3).toList().toBlocking().toFuture();
    final Future<List<EventSource.MessageEvent>> secondFuture =
        second.events().take(3).toList().toBlocking().toFuture();
    assertThat(firstFuture.get(), is(secondFuture.get()));
  }

  @Test
  public void shutdown_interrupts_event_streams() throws Exception {
    final AtomicBoolean terminated = new AtomicBoolean(false);
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        subject.destroy();
      }
    });
    monitor.events()
        .doOnTerminate(new Action0() {
          @Override
          public void call() {
            terminated.set(true);
          }
        })
        .subscribe();
    await().untilTrue(terminated);
  }

  @Test
  public void unsubscribing_does_not_stop_event_stream() throws Exception {
    final EventSource first = EventSource.listenTo(getServiceAddress(), Schedulers.newThread());
    final EventSource second = EventSource.listenTo(getServiceAddress(), Schedulers.newThread());
    second.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        for (int i = 0; i < 20; i++) {
          subject.publish(event(i + ""));
        }
      }
    });
    final Future<List<EventSource.MessageEvent>> firstFuture =
        first.events().take(3).toList().toBlocking().toFuture();
    final Future<List<EventSource.MessageEvent>> secondFuture =
        second.events().take(15).toList().toBlocking().toFuture();
    assertThat(firstFuture.get().size(), is(3));
    assertThat(secondFuture.get().size(), is(15));
  }

  @Test
  public void unsubscribing_removes_server_sided_listener() throws Exception {
    monitor.connection().subscribe(new Action1<HttpResponse>() {
      @Override
      public void call(final HttpResponse aVoid) {
        await().untilAtomic(subject.listenerCount(), is(1L));
      }
    });
    monitor.events().take(1).toBlocking().single();
    monitor.close();
    // trigger IO with comment to let app detect disconnected clients
    subject.publish(ServerSentEvent.Default.create(ServerSentEvent.COMMENT, "dummy"));
    await().untilAtomic(subject.listenerCount(), is(0L));
  }

  @Rule
  public ExpectedException error = ExpectedException.none();

  @Test
  public void subscription_rejected_if_max_listener_count_is_reached() throws Exception {
    subject.listenerCount().set(11);
    final AtomicReference<Throwable> error = new AtomicReference<>();
    monitor.events().subscribe(Actions.empty(), new Action1<Throwable>() {
      @Override
      public void call(final Throwable throwable) {
        error.set(throwable);
      }
    });
    //noinspection ThrowableResultOfMethodCallIgnored
    await().untilAtomic(error, is(instanceOf(EventSource.SubscriptionFailed.class)));
  }

  private URI getServiceAddress() {
    return tomcat.address().resolve("monitor");
  }

  private ServerSentEvent event(final String payload) {
    return ServerSentEvent.Default.create(ServerSentEvent.Type.valueOf("test"), payload);
  }
}
