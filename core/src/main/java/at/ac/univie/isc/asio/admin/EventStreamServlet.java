package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.config.AsioConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import rx.Observable;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publish {@link ServerSentEvent server events} as a HTTP
 * <a href="http://www.w3.org/TR/eventsource/">event stream</a>.
 */
@WebServlet(name = "event-stream-servlet", displayName = "Event Stream"
    , description = "Publish system and request events for monitoring"
    , urlPatterns = "/meta/events"
    , loadOnStartup = -1  // lazy initialization
    , asyncSupported = true
    , initParams = {})
public class EventStreamServlet extends HttpServlet {
  private static final Logger log = LoggerFactory.getLogger(EventStreamServlet.class);

  private final AtomicLong listenerCount = new AtomicLong(0);
  private int maxListenerCount = 10;
  private Observable<List<ServerSentEvent>> events;

  @VisibleForTesting
  EventStreamServlet(final Observable<List<ServerSentEvent>> events) {
    this.events = events;
  }

  public EventStreamServlet() { /* servlet container ctor */ }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    final WebApplicationContext spring =
        WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    this.events = spring.getBean(EventStream.class).observe();
    log.info(AsioConfiguration.SYSTEM, "ready");
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
    final ServletOutputStream servletOutputStream = context.getResponse().getOutputStream();
    final ServerSentEvent.Writer eventWriter = ServerSentEvent.Writer.wrap(servletOutputStream);
    final PushObservedEvents subscriber = PushObservedEvents.to(eventWriter);
    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        listenerCount.decrementAndGet();
      }
    }));
    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        context.complete();
      }
    }));
    events.subscribe(subscriber);
    listenerCount.incrementAndGet();
    log.debug("{}@{} subscribed to event stream", req.getRemoteUser(), req.getRemoteAddr());
  }

  private void populateHeaders(final HttpServletResponse resp) {
    resp.setContentType(ServerSentEvent.Writer.EVENT_STREAM_MIME);
    resp.setCharacterEncoding(ServerSentEvent.Writer.EVENT_STREAM_CHARSET.name());
    resp.setLocale(Locale.ENGLISH);
    // cache-control headers as in com.netflix.hystrix.[].HystrixMetricsStreamServlet
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0");
    resp.setHeader(HttpHeaders.PRAGMA, "no-cache");
    resp.setHeader(HttpHeaders.CONNECTION, "keep-alive");
  }
}
