package at.ac.univie.isc.asio;

import com.google.common.base.Charsets;

import java.io.ByteArrayInputStream;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

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

  /** convert raw UTF_8 encoded data back to String */
  public static String asString(final byte[] data) {
    return new String(data, Charsets.UTF_8);
  }
}
