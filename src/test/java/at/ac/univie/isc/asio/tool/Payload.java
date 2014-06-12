package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.transport.ObservableStream;
import rx.Observable;

import java.io.ByteArrayInputStream;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * @author pyranja
 */
public final class Payload {
  private static final Random RNG = new Random();

  private Payload() { /* no instances */ }

  public static byte[] randomWithLength(final int size) {
    checkArgument(size >= 0, "payload cannot have negative size");
    final byte[] payload = new byte[size];
    RNG.nextBytes(payload);
    return payload;
  }

  public static ByteArrayInputStream randomStreamWithLength(final int size) {
    return new ByteArrayInputStream(randomWithLength(size));
  }

  public static Observable<ObservableStream> observableFrom(final byte[] data) {
    requireNonNull(data);
    return Observable.just(ObservableStream.from(new ByteArrayInputStream(data)));
  }
}
