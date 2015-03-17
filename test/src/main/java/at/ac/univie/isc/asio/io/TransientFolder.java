package at.ac.univie.isc.asio.io;

import at.ac.univie.isc.asio.Unchecked;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Create a folder in the system's temporary-file directory and clear it, including all contents,
 * after the test.
 */
public final class TransientFolder extends ExternalResource {
  /**
   * Initialize the rule. The temporary folder is created lazily when a test starts.
   */
  public static TransientFolder create() {
    return new TransientFolder();
  }

  private Path folder;

  private TransientFolder() {}

  public Path path() {
    assert folder != null : "called outside of test context";
    return folder;
  }

  @Override
  protected void before() throws Throwable {
    assert folder == null : "transient folder already created";
    folder = Files.createTempDirectory("test-");
  }

  @Override
  protected void after() {
    if (folder != null && Files.exists(folder)) {
      Unchecked.run(new Unchecked.Action() {
        @Override
        public void call() throws Exception {
          Files.walkFileTree(folder, ClearFolder.instance());
        }
      });
    }
  }

  /**
   * Attempt to delete all contents if the walked path and the path itself.
   */
  private static final class ClearFolder extends SimpleFileVisitor<Path> {
    private ClearFolder() {}

    private static ClearFolder instance() {
      return new ClearFolder();
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
      // try deleting the folder in any case
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  }
}
