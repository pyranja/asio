package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.tool.Resources;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Turn an {@link Invocation} into a reactive sequence yielding the {@link Command.Results results}
 * on completion or an error.
 * <p>A single execution may only be subscribed to once. Subsequent subscriptions will fail
 * immediately.</p>
 * <p>To properly clean up utilized resources, the results <strong>must</strong>
 * either be consumed or closed.</p>
 */
@NotThreadSafe
@Nonnull
public class OnSubscribeExecute implements Observable.OnSubscribe<Command.Results> {

  private static enum State {EXECUTE, STREAM, COMPLETE, ABORT, DONE}
  /* null      => initial - not started
   * EXECUTE   => query is running
   * STREAM    => results are ready to be/are being serialized
   * COMPLETE  => result serialization finished successfully
   * ABORT     => query has been aborted before completing
   * DONE      => resources have been released
   */
  private final AtomicReference<State> state;
  private final Invocation delegate;

  public OnSubscribeExecute(final Invocation delegate) {
    this.delegate = requireNonNull(delegate);
    this.state = new AtomicReference<>();
  }

  @Override
  public void call(final Subscriber<? super Command.Results> subscriber) {
    try {
      prepare(subscriber);
      execute();
      stream(subscriber);
    } catch (final Exception e) {
      subscriber.onError(e);
    } finally {
      if (state.get() != State.STREAM) {
        cleanUp();  // if aborted/failed before subscriber consumed results
      }
    }
    subscriber.onCompleted();
  }

  private void prepare(final Subscriber<?> subscriber) {
    final boolean firstSubscription = state.compareAndSet(null, State.EXECUTE);
    assert firstSubscription : "multiple subscriptions";
    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        cancel(State.EXECUTE);
      }
    }));
  }

  private void execute() {
    if (state.get() != State.EXECUTE) { return; }
    delegate.execute();
  }

  private void stream(final Subscriber<? super Command.Results> subscriber) {
    if (state.get() != State.EXECUTE) { return; }
    subscriber.onNext(new Command.Results() {
      @Override
      public void write(final OutputStream output) throws IOException {
        state.compareAndSet(State.EXECUTE, State.STREAM);
        try {
          delegate.write(output);
          state.compareAndSet(State.STREAM, State.COMPLETE);
        } finally {
          Resources.close(this);
        }
      }

      @Override
      public MediaType format() {
        return delegate.produces();
      }

      @Override
      public void close() {
        try {
          cancel(State.STREAM);
        } finally {
          cleanUp();
        }
      }
    });
    state.compareAndSet(State.EXECUTE, State.STREAM);
  }

  private void cancel(State expectedPhase) {
    if (state.compareAndSet(expectedPhase, State.ABORT)) {
      delegate.cancel();
    }
  }

  private void cleanUp() {
    if (state.getAndSet(State.DONE) != State.DONE) {
      delegate.close();
    }
  }
}
