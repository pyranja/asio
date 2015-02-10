package at.ac.univie.isc.asio.io;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Copy all data written to the wrapped {@code OutputStream} into an in-memory buffer.
 */
public final class TeeOutputStream extends FilterOutputStream {
  /**
   * Wrap the given stream and capture all written data.
   * @param out original stream
   * @return wrapped stream
   */
  public static TeeOutputStream wrap(final OutputStream out) {
    return new TeeOutputStream(out);
  }

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private TeeOutputStream(final OutputStream out) {
    super(out);
  }

  @Override
  public void write(final int b) throws IOException {
    buffer.write(b);
    out.write(b);
  }

  @Override
  public void write(@Nonnull final byte[] b) throws IOException {
    buffer.write(b);
    out.write(b);
  }

  @Override
  public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
    buffer.write(b, off, len);
    out.write(b, off, len);
  }

  public byte[] captured() {
    return buffer.toByteArray();
  }
}
