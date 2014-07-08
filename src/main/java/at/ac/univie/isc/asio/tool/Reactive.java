package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.transport.ObservableStream;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import rx.Observable;
import rx.Subscriber;
import rx.functions.*;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

/**
 * RxJava support utilities.
 */
public final class Reactive {

  /** aliased, as compiler cannot infer from flatMap signature */
  public static final Func1<ObservableStream, ObservableStream> IDENTITY = Functions.identity();

  /** Reduce byte chunks to a single {@code byte[]}. */
  public static final ByteReducer BYTE_ACCUMULATOR = new ByteReducer();

  /** Write byte chunks to an {@code OutputStream}. */
  public static final StreamCollector STREAM_COLLECTOR = new StreamCollector();

  /**
   * Convert a {@code ListenableFuture} into an {@code Observable}.
   * The observable will emit a single item, the result of the future. Subscribing does not block.
   * The subscriber <strong>may</strong> be invoked synchronously on the subscribing thread or
   * asynchronously from the thread, which completes the future. Unsubscribing will
   * {@link com.google.common.util.concurrent.ListenableFuture#cancel(boolean) cancel} the future.
   *
   * @see com.google.common.util.concurrent.ListenableFuture
   * @see rx.internal.operators.OnSubscribeToObservableFuture
   *
   * @param future task which should be observed
   * @param <T> return type of the future
   * @return subscription function
   */
  public static <T> ToObservableListenableFuture<T> listeningFor(@Nonnull final ListenableFuture<T> future) {
    return new ToObservableListenableFuture<T>(future);
  }

  private Reactive() {
    /* utility class */
  }

  private static class ByteReducer implements Func2<byte[], byte[], byte[]> {
    @Override
    public byte[] call(final byte[] bytes, final byte[] bytes2) {
      return Bytes.concat(bytes, bytes2);
    }
  }

  private static final class StreamCollector implements Action2<OutputStream, byte[]> {
    @Override
    public void call(final OutputStream output, final byte[] bytes) {
      try {
        output.write(bytes);
      } catch (IOException e) {
        // FIXME : use custom RuntimeException
        throw new DatasetTransportException(e);
      }
    }
  }

  @VisibleForTesting
  static class ToObservableListenableFuture<T> implements Observable.OnSubscribe<T> {
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
}
