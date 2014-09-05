package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.tool.Duration;
import at.ac.univie.isc.asio.tool.Resources;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publish {@link at.ac.univie.isc.asio.admin.ServerSentEvent server events} as a HTTP
 * <a href="http://www.w3.org/TR/eventsource/">event stream</a>.
 */
@WebServlet(name = "MonitorServlet", urlPatterns = "/admin/events", asyncSupported = true, loadOnStartup = 10)
public final class EventMonitorServlet extends HttpServlet {
  private static final Logger log = LoggerFactory.getLogger(EventMonitorServlet.class);
  // TODO : use Observable#doOn(Un)Subscribe to track listener count
  // TODO : externalize buffer scheduler
  // TODO : externalize window parameters and max listener count

  public static final ServerSentEvent INITIAL_EVENT = ServerSentEvent.Default.create(
      ServerSentEvent.Type.valueOf("system")
      , "{\"message\":\"subscribed\"}"
  );
  public static final ServerSentEvent PING = ServerSentEvent.Default.create(
      ServerSentEvent.COMMENT
      , "ping"
  );
  private static final List<ServerSentEvent> PING_CHUNK = Collections.singletonList(PING);

  public static final Function<ServletContext, EventBus> SPRING_EVENT_BUS_RESOLVER =
      new Function<ServletContext, EventBus>() {
        @Override
        public EventBus apply(@Nullable final ServletContext context) {
          final WebApplicationContext spring =
              WebApplicationContextUtils.getRequiredWebApplicationContext(context);
          return spring.getBean(EventBus.class);
        }
      };

  private final Subject<ServerSentEvent, ServerSentEvent> publisher;
  private final Observable<List<ServerSentEvent>> events;
  private final Function<ServletContext, EventBus> eventBusResolver;

  private volatile Action0 cleanUp = Actions.empty();
  private final AtomicLong listenerCount = new AtomicLong(0);

  private Duration window;
  private int windowSize = 50;
  private int maxListenerCount = 10;

  @SuppressWarnings("UnusedDeclaration")  // used by servlet container
  public EventMonitorServlet() {
    this(Schedulers.io(), SPRING_EVENT_BUS_RESOLVER, Duration.create(1L, TimeUnit.SECONDS));
  }

  @VisibleForTesting
  EventMonitorServlet(final Scheduler scheduler, final Function<ServletContext, EventBus> eventBusResolver, final Duration window) {
    this.eventBusResolver = eventBusResolver;
    this.window = window;
    this.publisher = PublishSubject.create();
    this.events = publisher.asObservable()
        .startWith(INITIAL_EVENT)
        .buffer(window.length(), window.unit(), windowSize)
        .map(new Func1<List<ServerSentEvent>, List<ServerSentEvent>>() {
          @Override
          public List<ServerSentEvent> call(final List<ServerSentEvent> chunk) {
            return pingIfEmpty(chunk);
          }
        })
        .observeOn(scheduler)
    ;
  }

  private List<ServerSentEvent> pingIfEmpty(final List<ServerSentEvent> chunk) {
    if (chunk.isEmpty()) {
      return PING_CHUNK;
    } else {
      return chunk;
    }
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    final EventBus bus = eventBusResolver.apply(config.getServletContext());
    assert bus != null : "event bus resolved to null";
    bus.register(this);
    final EventMonitorServlet self = this; // capture self reference and bus for later disposal
    cleanUp = new Action0() {
      @Override
      public void call() {
        bus.unregister(self);
        log.info("[BOOT] unregistered from event bus");
      }
    };
    log.info("[BOOT] registered with event bus");
  }

  @Override
  public void destroy() {
    log.info("[BOOT] shutting down");
    publisher.onCompleted();
    cleanUp.call();
  }

  @VisibleForTesting
  AtomicLong listenerCount() {
    return listenerCount;
  }

  @Subscribe
  public void publish(final ServerSentEvent event) {
    log.trace("monitor received {}", event);
    publisher.onNext(event);
  }

  @Override
  protected void doHead(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    populateHeaders(resp);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    log.debug("{}@{} subscribes to event stream", req.getRemoteUser(), req.getRemoteAddr());
    if (listenerCount.get() + 1 > maxListenerCount) {
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "too many listeners connected");
      return;
    }
    populateHeaders(resp);
    final AsyncContext context = req.startAsync();
    // disable timeout as the event-stream is infinite - the response will not complete() on its own
    context.setTimeout(0);
    final EventChunkSubscriber subscriber = new EventChunkSubscriber(context);
    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        listenerCount.decrementAndGet();
      }
    }));
    events.subscribe(subscriber);
    listenerCount.incrementAndGet();
    log.debug("{}@{} subscribed to event stream", req.getRemoteUser(), req.getRemoteAddr());
  }

  private void populateHeaders(final HttpServletResponse resp) {
    resp.setContentType("text/event-stream");
    resp.setCharacterEncoding(Charsets.UTF_8.name());
    resp.setLocale(Locale.ENGLISH);
    // cache-control headers as in com.netflix.hystrix.[].HystrixMetricsStreamServlet
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0");
    resp.setHeader(HttpHeaders.PRAGMA, "no-cache");
    resp.setHeader(HttpHeaders.CONNECTION, "keep-alive");
  }

  @VisibleForTesting
  void writeEvent(final ServerSentEvent event, final Writer sink) throws IOException {
    if (event.type() == ServerSentEvent.COMMENT) {
      // special case comment lines
      sink.write(":");
      event.writeTo(sink);
      sink.write("\n"); // do not terminate with empty line to avoid forcing an invalid dispatch
    } else {  // a valid event
      if (event.type() != ServerSentEvent.GENERIC) {
        // add "event" field for typed events
        sink.write("event:");
        sink.write(event.type().toString());
        sink.write("\n");
      }
      sink.write("data:");
      event.writeTo(sink);
      sink.write("\n\n");
    }
  }

  private class EventChunkSubscriber extends Subscriber<List<ServerSentEvent>> {
    private final Writer sink;

    public EventChunkSubscriber(final AsyncContext context) throws IOException {
      add(Subscriptions.create(new Action0() {
        @Override
        public void call() {
          context.complete();
        }
      }));
      sink = new OutputStreamWriter(context.getResponse().getOutputStream(), Charsets.UTF_8);
    }

    @Override
    public void onCompleted() {
      log.warn("server event publisher ended");
    }

    @Override
    public void onError(final Throwable e) {
      log.warn("server event publisher failed", e);
    }

    @Override
    public void onNext(final List<ServerSentEvent> events) {
      log.trace("subscriber received {}", events);
      try {
        for (ServerSentEvent each : events) {
          writeEvent(each, sink);
        }
        sink.flush();
      } catch (IOException error) {
        unsubscribe();
        if (Resources.indicatesClientDisconnect(error)) {
          log.debug("subscriber disconnected");
        } else {
          log.error("writing event failed", error);
        }
      }
    }
  }
}
