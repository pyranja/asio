package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

import javax.annotation.concurrent.Immutable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reactive Server-Sent-Events client.
 *
 * @see <a href="http://www.w3.org/TR/2011/WD-eventsource-20111020/">W3C SSE spec</a>
 */
public final class EventSource implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(EventSource.class);

  /**
   * Accepted SSE mime type
   */
  public static final MediaType EVENT_STREAM_MIME =
      MediaType.create("text", "event-stream").withoutParameters();

  /**
   * Create an EventSource connecting in the IO scheduler to given endpoint.
   *
   * @param target URI of event stream endpoint
   * @return created instance
   */
  public static EventSource listenTo(final URI target) {
    return new EventSource(target, Schedulers.io());
  }

  /**
   * Create an EventSource connecting on given scheduler to given endpoint.
   *
   * @param target    URI of event stream endpoint
   * @param scheduler to be used for connecting
   * @return created instance
   */
  public static EventSource listenTo(final URI target, final Scheduler scheduler) {
    return new EventSource(target, scheduler);
  }

  /**
   * connecting to the event stream server failed
   */
  public static class SubscriptionFailed extends IOException {
    public SubscriptionFailed(final HttpResponse response) {
      super("subscription to event stream failed : " + response.getStatusLine().toString());
    }
  }


  /**
   * this event source is closed
   */
  public static class EventSourceClosed extends IOException {
    public EventSourceClosed() {
      super("EventSource closed");
    }
  }


  private static final Pattern EVENT_PATTERN = Pattern.compile("^(\\w+):(.*)$");

  private final HttpGet request;
  private final Observable<MessageEvent> events;
  private final Subject<Void, Void> connection;
  private volatile boolean closed;

  private EventSource(final URI target, final Scheduler scheduler) {
    request = new HttpGet(target);
    request.setHeader(HttpHeaders.ACCEPT, EVENT_STREAM_MIME.toString());
    request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    connection = PublishSubject.create();
    events = Observable.create(new ConsumeEventsOnSubscribe()).subscribeOn(scheduler).share();
  }

  /**
   * Emits a {@code null} item, whenever this source connects to the event server.
   *
   * @return observable connection events
   */
  public Observable<Void> connection() {
    return connection;
  }

  /**
   * Emits all server sent events received.
   *
   * @return observable event stream
   */
  public Observable<MessageEvent> events() {
    return events;
  }

  /**
   * Dispose this EventSource, rendering it unusable.
   */
  @Override
  public void close() {
    closed = true;
    request.abort();
    connection.onCompleted();
  }

  private class ConsumeEventsOnSubscribe implements Observable.OnSubscribe<MessageEvent> {
    private final StringBuilder data = new StringBuilder();
    private final MessageEvent.Builder event = MessageEvent.create();
    private String line;
    private Subscriber<? super MessageEvent> eventObserver;

    @Override
    public void call(final Subscriber<? super MessageEvent> subscriber) {
      if (closed) {
        subscriber.onError(new EventSourceClosed());
        return;
      }
      log.debug("attempt connection to event stream");
      eventObserver = subscriber;
      eventObserver.add(Subscriptions.create(new Action0() {
        @Override
        public void call() {
          request.abort();
        }
      }));
      final HttpClient client = createClient();
      request.reset();
      try {
        final HttpResponse response = client.execute(request);
        log.debug("subscription response {}", response.getStatusLine());
        if (closed || eventObserver.isUnsubscribed()) {
          throw new EventSourceClosed();
        }
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          log.debug("connection established");
          connection.onNext(null);
          consume(response);
          log.debug("event stream consumed");
          eventObserver.onCompleted();
        } else {
          throw new SubscriptionFailed(response);
        }
      } catch (IOException e) {
        notify(e);
      } finally {
        client.getConnectionManager().shutdown();
      }
    }

    private void consume(final HttpResponse response) throws IOException {
      try (
          final InputStream httpStream = response.getEntity().getContent();
          final BufferedReader eventStream =
              new BufferedReader(new InputStreamReader(httpStream, Charsets.UTF_8))
      ) {
        while ((line = eventStream.readLine()) != null && !eventObserver.isUnsubscribed()
            && !closed) {
          if (line.isEmpty()) {
            dispatch();
          } else {
            parse();
          }
        }
      }
    }

    private void notify(final IOException e) {
      log.error("event stream failed", e);
      if (!eventObserver.isUnsubscribed()) {
        eventObserver.onError(e);
      }
    }

    private void dispatch() {
      if (data.length() > 0) {
        stripTrailingNewline();
        final MessageEvent event = this.event.withData(data.toString());
        log.trace("dispatching {}", event);
        eventObserver.onNext(event);
      }
      data.setLength(0);
      event.reset();
    }

    private void stripTrailingNewline() {
      final int lastIndex = data.length() - 1;
      if (data.charAt(lastIndex) == '\n') {
        data.setLength(lastIndex);
      }
    }

    private void parse() {
      final Matcher matcher = EVENT_PATTERN.matcher(line);
      if (matcher.matches()) {
        final String field = matcher.group(1);
        final String value = matcher.group(2).trim();
        switch (field.toLowerCase(Locale.ENGLISH)) {
          case "id":
            event.withId(value);
            break;
          case "event":
            event.withType(value);
            break;
          case "data":
            data.append(value).append('\n');
            break;
        }
      }
    }
  }

  private HttpClient createClient() {
    final DefaultHttpClient client = new DefaultHttpClient();
    // FIXME : totally unsafe
    // final SSLContext ssl = SSLContext.getInstance("TLS");
    // ssl.init(null, new TrustManager[] { UNSAFE_TRUST_MANAGER }, null);
    final SSLSocketFactory factory;
    try {
      factory =
          new SSLSocketFactory(new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, factory));
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException e) {
      Throwables.propagate(e);
    }
    return client;
  }

  @Immutable
  public static class MessageEvent {
    private final Optional<String> id;
    private final String type;
    private final String data;

    MessageEvent(final Optional<String> id, final String type, final String data) {
      this.id = id;
      this.type = type;
      this.data = data;
    }

    public Optional<String> id() {
      return id;
    }

    public String type() {
      return type;
    }

    public String data() {
      return data;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .omitNullValues()
          .add("id", id.orNull())
          .add("type", type)
          .add("data", data)
          .toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final MessageEvent that = (MessageEvent) o;
      return data.equals(that.data) && id.equals(that.id) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + type.hashCode();
      result = 31 * result + data.hashCode();
      return result;
    }

    public static Builder create() {
      return new Builder();
    }

    public static class Builder {
      private Optional<String> id = Optional.absent();
      private String type = "message";

      public Builder reset() {
        this.id = Optional.absent();
        this.type = "message";
        return this;
      }

      public Builder withId(final String id) {
        this.id = Optional.of(id);
        return this;
      }

      public Builder withType(final String type) {
        this.type = type;
        return this;
      }

      public MessageEvent withData(final String data) {
        return new MessageEvent(id, type, data);
      }
    }
  }
}
