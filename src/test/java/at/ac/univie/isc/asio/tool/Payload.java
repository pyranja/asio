package at.ac.univie.isc.asio.tool;

import java.io.ByteArrayInputStream;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

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
}
