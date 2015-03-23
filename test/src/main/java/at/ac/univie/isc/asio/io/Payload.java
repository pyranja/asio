package at.ac.univie.isc.asio.io;

import at.ac.univie.isc.asio.Unchecked;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Aid in generating and using binary data.
 */
public final class Payload {
  private static final Random RNG = new Random();

  private Payload() { /* no instances */ }

  /**
   * Generate a fixed size {@code byte[]} with random contents.
   * @param size of generated array
   * @return randomly filled byte array
   */
  public static byte[] randomWithLength(final int size) {
    assert size >= 0 : "payload cannot have negative size";
    final byte[] payload = new byte[size];
    RNG.nextBytes(payload);
    return payload;
  }

  /**
   * Convert raw utf-8 encoded data back to {@code String}. It is not checked whether the data is actually
   * utf-8 encoded.
   * @param data raw binary data of an utf-8 encoded string
   * @return decoded data as String
   */
  public static String decodeUtf8(final byte[] data) {
    return new String(data, StandardCharsets.UTF_8);
  }

  /**
   * Convert a {@code String} to utf-8 encoded data.
   * @param text to be converted
   * @return utf-8 encoded binary data
   */
  public static byte[] encodeUtf8(final String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Copy the content from given {@code ByteSource} to an array.
   * @param source source of binary data
   * @return byte array containing the contents of the {@code source}
   * @throw UncheckedIOException if reading the source fails
   */
  public static byte[] asArray(final ByteSource source) {
    try {
      return source.read();
    } catch (IOException e) {
      throw new Unchecked.UncheckedIOException(e);
    }
  }
}
