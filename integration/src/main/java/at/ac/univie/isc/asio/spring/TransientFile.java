package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.Unchecked;
import com.google.common.io.ByteSource;
import org.junit.rules.ExternalResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dump packaged data into a temporary file during a test run.
 */
public final class TransientFile extends ExternalResource {
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
  protected void before() throws Throwable {
    dump = Files.createTempFile("keystore-dump-", ".jks");
    Files.write(dump, content.read());
  }

  @Override
  protected void after() {
    if (dump != null) {
      Unchecked.run(new Unchecked.Action() {
        @Override
        public void call() throws Exception {
          Files.deleteIfExists(dump);
        }
      });
    }
  }
}
