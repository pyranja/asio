package at.ac.univie.isc.asio.transport;

import at.ac.univie.isc.asio.common.Resources;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * An observable sequence of binary data, emitting {@code byte[]} chunks. Subscribing to the {@code
 * ObservableStream} <strong>may</strong> consume the backing data source.
 */
public final class ObservableStream extends Observable<byte[]> {

  /**
   * The size of emitted chunks will never exceed this value. A best effort is made to emit chunks
   * with exactly this size.
   */
  @VisibleForTesting
  static final int MAX_CHUNK_SIZE = 8192;

  /**
   * Use the given {@code InputStream} to read and emit {@code byte[]} chunks on subscription. The
   * emitted chunks have a fixed size limit. After consumption {@code source} is closed.
   *
   * @param source data source
   * @return an {@code Observable} emitting the contents of {@code source} as {@code byte[]} chunks
   */
  public static ObservableStream from(final InputStream source) {
    return new ObservableStream(new OnSubscribeConsumeStream(source));
  }

  /**
   * Wrap the given {@code Observable} with an {@code ObservableStream}.
   * All events emitted by {@code inner} are forwarded as is.
   * @param inner wrapped {@code Observable}
   * @return an {@code ObservableStream}
   */
  public static ObservableStream wrap(final Observable<byte[]> inner) {
    return new ObservableStream(new OnSubscribe<byte[]>() {
      @Override
      public void call(final Subscriber<? super byte[]> subscriber) {
        if (!subscriber.isUnsubscribed()) {
          final Subscription forward = inner.unsafeSubscribe(subscriber);
          subscriber.add(forward);
        }
      }
    });
  }

  private ObservableStream(final OnSubscribe<byte[]> f) {
    super(f);
  }

  @VisibleForTesting
  static final class OnSubscribeConsumeStream implements OnSubscribe<byte[]> {

    private final InputStream stream;

    private OnSubscribeConsumeStream(final InputStream stream) {
      this.stream = stream;
    }

    @Override
    public void call(final Subscriber<? super byte[]> subscriber) {
      subscriber.add(Subscriptions.create(new Action0() {
        @Override
        public void call() {
          Resources.close(stream);
        }
      }));
      try (final InputStream source = stream) {
        while (!subscriber.isUnsubscribed()) {
          byte[] buffer = new byte[MAX_CHUNK_SIZE];
          int lastBatchSize = ByteStreams.read(source, buffer, 0, MAX_CHUNK_SIZE);
          if (lastBatchSize == 0) {
            break;
          }
          if (lastBatchSize < MAX_CHUNK_SIZE) {  // compact on partial read
            subscriber.onNext(Arrays.copyOfRange(buffer, 0, lastBatchSize));
          } else {
            subscriber.onNext(buffer);
          }
        }
        subscriber.onCompleted();
      } catch (IOException e) {
        subscriber.onError(e);
      }
    }
  }
}
