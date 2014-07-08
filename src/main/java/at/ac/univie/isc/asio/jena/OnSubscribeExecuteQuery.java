package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.transport.ObservableStream;
import at.ac.univie.isc.asio.transport.SubscribedOutputStream;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.shared.JenaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Turn a {@code QueryExecution} into a reactive sequence of nested {@code Observables}. The
 * outermost one yields a single {@link at.ac.univie.isc.asio.transport.ObservableStream}, as soon
 * as the query is executed. The inner {@code Observable} yields the serialized results as a series
 * of {@code byte[]} chunks.
 * <p>A single execution may only be subscribed to once. Subsequent subscriptions will fail
 * immediately.</p>
 * <p>To properly clean up utilized resources, the {@code ObservableStream} <strong>must</strong>
 * either be consumed or unsubscribed from, even if query execution is aborted.</p>
 */
@NotThreadSafe
@Nonnull
final class OnSubscribeExecuteQuery implements Observable.OnSubscribe<ObservableStream> {
  private static final Logger log = LoggerFactory.getLogger(OnSubscribeExecuteQuery.class);

  private final QueryExecution query;
  private final JenaQueryHandler handler;

  private static enum State { EXECUTE, STREAM, ABORT, DONE }
  /* null      => initial - not started
   * EXECUTE   => query is running
   * STREAM    => results are ready to be/are being serialized
   * DONE      => resources have been released
   * ABORT     => query has been aborted before completing
   */
  private final AtomicReference<State> state;

  public OnSubscribeExecuteQuery(final QueryExecution query, final JenaQueryHandler handler) {
    this.query = requireNonNull(query);
    this.handler = requireNonNull(handler);
    this.state = new AtomicReference<>();
  }

  @Override
  public void call(final Subscriber<? super ObservableStream> executionConsumer) {
    try {
      prepare(executionConsumer);
      execute();
      stream(executionConsumer);
    } catch (final Exception e) {
      executionConsumer.onError(e);
    } finally {
      if (state.get() != State.STREAM) {
        cleanUp();
      }
    }
    executionConsumer.onCompleted();
  }

  private void prepare(final Subscriber<? super ObservableStream> executionConsumer) {
    final boolean firstSubscription = state.compareAndSet(null, State.EXECUTE);
    assert firstSubscription : "multiple subscriptions";
    executionConsumer.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        cancel(State.EXECUTE);
      }
    }));
  }

  private void execute() {
    if (state.get() != State.EXECUTE) { return; }
    handler.invoke(query);
  }

  private void stream(final Subscriber<? super ObservableStream> executionConsumer) {
    if (state.get() != State.EXECUTE) { return; }
    executionConsumer.onNext(
        ObservableStream.make(new Observable.OnSubscribe<byte[]>() {
          @Override
          public void call(final Subscriber<? super byte[]> streamConsumer) {
            state.compareAndSet(State.EXECUTE, State.STREAM);
            streamConsumer.add(Subscriptions.create(new Action0() {
              @Override
              public void call() {
                cancel(State.STREAM);
              }
            }));
            try (final OutputStream sink = new SubscribedOutputStream(streamConsumer)) {
              handler.serialize(sink);
            } catch (JenaException | IOException e) {
              streamConsumer.onError(e);
            } finally {
              cleanUp();
            }
            streamConsumer.onCompleted();
          }
        })
    );
    state.compareAndSet(State.EXECUTE, State.STREAM);
  }

  private void cancel(State expectedPhase) {
    if (state.compareAndSet(expectedPhase, State.ABORT)) {
      query.abort();
    }
  }

  private void cleanUp() {
    if (state.getAndSet(State.DONE) != State.DONE) {
      query.close();
    }
  }

}
