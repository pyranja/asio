package at.ac.univie.isc.asio.common;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

/**
 * Convert a {@code ListenableFuture} into an {@code Observable}.
 * The observable will emit a single item, the result of the future. Subscribing does not block.
 * The subscriber <strong>may</strong> be invoked synchronously on the subscribing thread or
 * asynchronously from the thread, which completes the future. Unsubscribing will
 * {@link com.google.common.util.concurrent.ListenableFuture#cancel(boolean) cancel} the future.
 *
 * @see com.google.common.util.concurrent.ListenableFuture
 * @see rx.internal.operators.OnSubscribeToObservableFuture
 * @param <T> return type of the future
 */
public class ToObservableListenableFuture<T> implements Observable.OnSubscribe<T> {
  public static <T> ToObservableListenableFuture<T> listeningFor(@Nonnull final ListenableFuture<T> future) {
    return new ToObservableListenableFuture<T>(future);
  }

  private final ListenableFuture<T> future;

  private ToObservableListenableFuture(final ListenableFuture<T> future) {
    this.future = requireNonNull(future);
  }

  @Override
  public void call(final Subscriber<? super T> subscriber) {
    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        future.cancel(true);
      }
    }));
    final FutureCallback<T> callback = new FutureCallback<T>() {
      @Override
      public void onSuccess(@Nullable final T result) {
        if (subscriber.isUnsubscribed()) { return; }
        subscriber.onNext(result);
        subscriber.onCompleted();
      }

      @Override
      public void onFailure(final Throwable t) {
        if (subscriber.isUnsubscribed()) { return; }
        Throwable cause = t;
        if (t instanceof ExecutionException) {
          cause = (t.getCause() != null) ? t.getCause() : t;
        }
        subscriber.onError(cause);
      }
    };
    if (!subscriber.isUnsubscribed()) {
      Futures.addCallback(future, callback, MoreExecutors.sameThreadExecutor());
    }
  }
}
