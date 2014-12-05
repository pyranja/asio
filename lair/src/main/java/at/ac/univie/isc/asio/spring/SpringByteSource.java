package at.ac.univie.isc.asio.spring;

import com.google.common.io.ByteSource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Adapt a Spring {@code Resource} to the Guava {@code ByteSource} contract.
 */
public final class SpringByteSource extends ByteSource {
  /**
   * Wrap a spring resource as ByteSource.
   * @param delegate spring resource
   * @return wrapping ByteSource
   */
  public static SpringByteSource asByteSource(final Resource delegate) {
    requireNonNull(delegate);
    return new SpringByteSource(delegate);
  }

  private final Resource delegate;

  private SpringByteSource(final Resource delegate) {
    this.delegate = delegate;
  }

  @Override
  public InputStream openStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public String toString() {
    return delegate.getDescription();
  }
}
