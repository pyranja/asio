package at.ac.univie.isc.asio.io;

import at.ac.univie.isc.asio.Unchecked;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Load a file from the test classpath and handle safe provisioning of the data as a
 * {@link ByteSource}. Search the resource first with the given name, if that fails, also search
 * with an absolute version of the given name.
 */
public final class Classpath {

  /**
   * Load and stream a classpath resource from the root.
   *
   * @param name reference to a file
   * @return contents of the resource as a stream
   * @throws at.ac.univie.isc.asio.Unchecked.UncheckedIOException if opening the resource fails
   */
  @Nonnull
  public static InputStream fetch(@Nonnull final String name) {
    try {
      return load(name).openStream();
    } catch (IOException e) {
      throw new Unchecked.UncheckedIOException(e);
    }
  }

  /**
   * Load and read a classpath resource from the root. The resource must be encoded as UTF-8.
   *
   * @param name reference to the resource
   * @return contents of the resource as a string
   * @throws at.ac.univie.isc.asio.Unchecked.UncheckedIOException if reading the resource fails
   */
  @Nonnull
  public static String read(final String name) {
    try {
      return load(name).asCharSource(Charsets.UTF_8).read();
    } catch (IOException e) {
      throw new Unchecked.UncheckedIOException(e);
    }
  }

  /**
   * Load a classpath resource into a byte array.
   *
   * @param name reference to the resource
   * @return contents of the resource as a byte array
   * @throws at.ac.univie.isc.asio.Unchecked.UncheckedIOException if reading the resource fails
   */
  @Nonnull
  public static byte[] toArray(@Nonnull final String name) {
    try {
      return load(name).read();
    } catch (IOException e) {
      throw new Unchecked.UncheckedIOException(e);
    }
  }

  /**
   * Load a resource from the classpath and wrap it as a {@code ByteSource}.
   *
   * @param name name of the resource
   * @return the wrapped resource
   * @throws java.lang.IllegalArgumentException if the resource is not found
   */
  @Nonnull
  public static ByteSource load(@Nonnull final String name) throws IllegalArgumentException {
    Preconditions.checkArgument(!name.startsWith("/"), "illegal resource name <%s> - resource names may not have a leading slash, they are implicitly absolute", name);
    final URL url = Resources.getResource(name);
    return Resources.asByteSource(url);
  }
}
