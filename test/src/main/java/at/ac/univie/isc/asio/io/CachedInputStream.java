package at.ac.univie.isc.asio.io;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Add {@link #cached()} method to {@code ByteArrayInputStream}.
 */
public final class CachedInputStream extends ByteArrayInputStream {
  /**
   * Consume the given {@code InputStream} and cache received data for replay.
   * @param source original stream
   * @return stream, which replays cached data
   * @throws IOException on any error while reading source
   */
  public static CachedInputStream cache(final InputStream source) throws IOException {
    try (final InputStream ignored = source) {
      final byte[] data = ByteStreams.toByteArray(source);
      return new CachedInputStream(data);
    }
  }

  private CachedInputStream(final byte[] buf) {
    super(buf);
  }

  /**
   * @return cached data
   */
  public byte[] cached() {
    return buf;
  }
}
