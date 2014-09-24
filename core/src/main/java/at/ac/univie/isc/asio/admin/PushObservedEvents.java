package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.tool.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import java.io.IOException;
import java.util.Collection;

/**
 * Write received {@link at.ac.univie.isc.asio.admin.ServerSentEvent events} to an
 * {@link at.ac.univie.isc.asio.admin.ServerSentEvent.Writer event-stream}.
 * The wrapped {@code event-stream} will not be closed.
 */
class PushObservedEvents extends Subscriber<Collection<? extends ServerSentEvent>> {
  private static final Logger log = LoggerFactory.getLogger(PushObservedEvents.class);

  private final ServerSentEvent.Writer sink;

  public static PushObservedEvents to(final ServerSentEvent.Writer sink) {
    return new PushObservedEvents(sink);
  }

  private PushObservedEvents(final ServerSentEvent.Writer sink) {
    this.sink = sink;
  }

  @Override
  public void onCompleted() {
    log.warn("server event publisher ended");
    notify(EventStream.endOfStream());
  }

  @Override
  public void onError(final Throwable e) {
    log.warn("server event publisher failed", e);
    notify(EventStream.error(e));
  }

  @Override
  public void onNext(final Collection<? extends ServerSentEvent> chunk) {
    try {
      if (chunk.isEmpty()) {
        sink.comment("ping");
      } else {
        for (ServerSentEvent event : chunk) {
          push(event);
        }
      }
      sink.flush();
    } catch (IOException error) {
      unsubscribe();
      reportError(error);
    }
  }

  /** add an event to the stream */
  private void push(final ServerSentEvent event) throws IOException {
    sink.event(event.type());
    sink.data(event.data());
    sink.boundary();
  }

  /** send a stream notification and ignore IO errors */
  private void notify(final ServerSentEvent event) {
    try {
      push(event);
      sink.flush();
    } catch (IOException ignored) {
    }
  }

  /** log unexpected connection error */
  private void reportError(final IOException error) {
    if (Resources.indicatesClientDisconnect(error)) {
      log.debug("subscriber disconnected");
    } else {
      log.error("writing event failed", error);
    }
  }

}
