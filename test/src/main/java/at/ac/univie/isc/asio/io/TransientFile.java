package at.ac.univie.isc.asio.io;

import at.ac.univie.isc.asio.Unchecked;
import com.google.common.io.ByteSource;
import org.junit.rules.ExternalResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dump packaged data into a temporary file during a test run.
 */
public final class TransientFile extends ExternalResource implements AutoCloseable {
  /**
   * Create a rule, that creates a temporary file with given content before each test and deletes
   * it afterwards.
   *
   * @param content source of file content
   * @return the initialized rule
   */
  public static TransientFile from(final ByteSource content) {
    return new TransientFile(content);
  }

  /**
   * Create the transient file immediately (cannot be used as rule).
   */
  public static TransientFile create(final ByteSource content) {
    return new TransientFile(content).init();
  }

  private final ByteSource content;
  private Path dump;

  private TransientFile(final ByteSource content) {
    this.content = content;
  }

  /**
   * Path to the transient file.
   *
   * @return path to the transient file.
   */
  public Path path() {
    assert dump != null : "called outside of test context";
    return dump;
  }

  @Override
  public void close() {
    if (dump != null) {
      Unchecked.run(new Unchecked.Action() {
        @Override
        public void call() throws Exception {
          Files.deleteIfExists(dump);
        }
      });
    }
  }

  public TransientFile init() {
    assert dump == null : "already initialized";
    Unchecked.run(new Unchecked.Action() {
      @Override
      public void call() throws Exception {
        dump = Files.createTempFile("file-dump-", ".tmp");
        Files.write(dump, content.read());
      }
    });
    return this;
  }

  @Override
  protected void before() {
    init();
  }

  @Override
  protected void after() {
    close();
  }
}
