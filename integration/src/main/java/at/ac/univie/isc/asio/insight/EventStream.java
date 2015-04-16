package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Unchecked;
import com.google.common.base.Throwables;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Connect to a SSE endpoints and provide received events to subscriber.
 */
public final class EventStream {

  /**
   * Create an {@code Observable}, that will connect to the given server-sent-event endpoint on
   * subscription and then yields each received event until unsubscribed from. This observable is
   * <strong>hot</strong>, it will not complete on its own. Events are processed in a separate
   * jersey-internal thread.
   *
   * @param source jersey event source
   * @return observable that will yield all events received from the server
   */
  public static Observable<InboundEvent> listenTo(final EventSource source) {
    return Observable.create(new Observable.OnSubscribe<InboundEvent>() {
      @Override
      public void call(final Subscriber<? super InboundEvent> subscriber) {
        subscriber.add(Subscriptions.create(new Action0() {
          @Override
          public void call() {
            source.close(0, TimeUnit.SECONDS);
          }
        }));
        try {
          source.register(new EventListener() {
            @Override
            public void onEvent(final InboundEvent event) {
              subscriber.onNext(event);
            }
          });
          if (!source.isOpen()) {
            source.open();
          }
        } catch (final Exception e) {
          subscriber.onError(e);
        }
      }
    });
  }

  /**
   * Subscribe to the given {@link Observable} and collect all emitted items.
   * {@link Iterable#iterator() Iterating} the collected items
   *
   * @param source observable source of items
   * @param <ELEMENTS> type of collected items
   * @return an {@code Iterable} holding all collected items
   */
  public static <ELEMENTS> Iterable<ELEMENTS> collectAll(final Observable<ELEMENTS> source) {
    final Collector<ELEMENTS> collector = new Collector<>();
    source.subscribe(collector);
    return collector;
  }

  private static final class Collector<ELEMENTS> implements Iterable<ELEMENTS>, Observer<ELEMENTS> {
    private final Queue<ELEMENTS> received = new ConcurrentLinkedQueue<>();
    private final CountDownLatch finished = new CountDownLatch(1);
    private Throwable error = null;

    @Override
    public Iterator<ELEMENTS> iterator() {
      Unchecked.await(finished);
      if (error != null) {
        Throwables.propagate(error);
      }
      return received.iterator();
    }

    @Override
    public void onCompleted() {
      finished.countDown();
    }

    @Override
    public void onError(final Throwable e) {
      error = e;
      finished.countDown();
    }

    @Override
    public void onNext(final ELEMENTS event) {
      received.add(event);
    }

    @Override
    public String toString() {
      return "Collector{" +
          "received=" + received +
          ", error=" + error +
          '}';
    }
  }
}
