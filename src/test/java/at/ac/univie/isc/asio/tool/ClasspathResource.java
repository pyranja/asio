package at.ac.univie.isc.asio.tool;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;

import java.net.URL;

import org.junit.rules.ExternalResource;

import com.google.common.base.Supplier;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

/**
 * Load a file from the test classpath and handle safe provisioning of the data as a
 * {@link ByteSource}. Search the resource first with the given name, if that fails, also search
 * with an absolute version of the given name.
 * 
 * @author Chris Borckholder
 */
public class ClasspathResource extends ExternalResource implements Supplier<ByteSource> {

  /**
   * @param path absolute or relative reference to a file on the classpath
   * @return the prepared rule.
   */
  public static ClasspathResource load(final String path) {
    checkNotNull(emptyToNull(path), "illegal classpath reference : %s", path);
    return new ClasspathResource(path);
  }

  /**
   * @param path reference to a file - will be made absolute if it is relative
   * @return the prepared rule.
   */
  public static ClasspathResource fromRoot(final String path) {
    checkNotNull(emptyToNull(path), "illegal classpath reference : %s", path);
    if (path.startsWith("/")) {
      return load(path);
    } else {
      return load("/" + path);
    }
  }

  private final String name;
  private ByteSource resource;

  ClasspathResource(final String name) {
    super();
    this.name = name;
  }

  @Override
  public ByteSource get() {
    checkState(resource != null, "resource not loaded : maybe missing @Rule ?");
    return resource;
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    final URL path = this.getClass().getResource(name);
    checkNotNull(path, "resource %s not found", name);
    resource = Resources.asByteSource(path);
  }
}
