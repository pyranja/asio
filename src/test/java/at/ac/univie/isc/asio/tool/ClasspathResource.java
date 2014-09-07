package at.ac.univie.isc.asio.tool;

import com.google.common.base.Supplier;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * Load a file from the test classpath and handle safe provisioning of the data as a
 * {@link ByteSource}. Search the resource first with the given name, if that fails, also search
 * with an absolute version of the given name.
 */
public final class ClasspathResource implements Supplier<ByteSource> {

  /**
   * @param path absolute or relative reference to a file on the classpath
   * @return the resource.
   */
  public static ClasspathResource load(final String path) {
    checkNotNull(emptyToNull(path), "illegal classpath reference : %s", path);
    return new ClasspathResource(path);
  }

  /**
   * @param path reference to a file - will be made absolute if it is relative
   * @return the resource.
   */
  public static ClasspathResource fromRoot(final String path) {
    checkNotNull(emptyToNull(path), "illegal classpath reference : %s", path);
    if (path.startsWith("/")) {
      return load(path);
    } else {
      return load("/" + path);
    }
  }

  private final ByteSource resource;

  ClasspathResource(final String name) {
    final URL path = this.getClass().getResource(name);
    checkNotNull(path, "resource %s not found", name);
    resource = Resources.asByteSource(path);
  }

  @Override
  public ByteSource get() {
    return resource;
  }
}
