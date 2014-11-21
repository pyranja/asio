package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.tool.Duration;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.Collections;
import java.util.List;

/**
 * Publish {@link ServerSentEvent}s from an
 * {@link com.google.common.eventbus.EventBus} as an {@link rx.Observable observable sequence}.
 */
public final class EventStream {
  // ========== stream event factories
  public static ServerSentEvent subscribed() {
    return ServerSentEvent.Simple.create("stream", "subscribed");
  }
  public static ServerSentEvent endOfStream() {
    return ServerSentEvent.Simple.create("stream", "eof");
  }
  public static ServerSentEvent error(final Throwable cause) {
    return ServerSentEvent.Simple.create("stream", cause.getMessage());
  }

  private final Subject<ServerSentEvent, ServerSentEvent> stream;

  private final Scheduler scheduler;
  private final Duration window;
  private final int windowSize;

  public EventStream(final Scheduler scheduler, final Duration window, final int bufferSize) {
    this.scheduler = scheduler;
    this.window = window;
    this.stream = PublishSubject.create();
    windowSize = bufferSize;
  }

  @AllowConcurrentEvents
  @Subscribe
  public void publish(final ServerSentEvent event) {
    stream.onNext(event);
  }

  public Observable<List<ServerSentEvent>> observe() {
    return stream.asObservable()
        .buffer(window.length(), window.unit(), windowSize)
        .startWith(Collections.singletonList(subscribed()))
        .observeOn(scheduler)
        ;
  }
}
