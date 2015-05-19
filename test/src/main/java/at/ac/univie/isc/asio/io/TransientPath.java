/*
 * #%L
 * asio test
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.io;

import at.ac.univie.isc.asio.Unchecked;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

/**
 * Manage a {@link java.nio.file.Path} in the system's temporary directory. On {@code close} the
 * path is removed, if the path represents a directory, that directory is recursively deleted with
 * all its contents.
 * <p>Supports usage as a {@link org.junit.rules.TestRule JUnit rule}.</p>
 */
public final class TransientPath implements TestRule, AutoCloseable {
  /**
   * Create transient path as directory. The directory is created lazily, when calling
   * {@link #init()} or when a test starts, if used as a junit rule.
   */
  public static TransientPath folder() {
    return new TransientPath(new Callable<Path>() {
      @Override
      public Path call() throws Exception {
        return Files.createTempDirectory("test-");
      }
    });
  }

  /**
   * Create transient path as file. The file is created and filled lazily, when calling
   * {@link #init()} or when a test starts, if used as a junit rule.
   *
   * @param content required content of the file
   */
  public static TransientPath file(final byte[] content) {
    return new TransientPath(new Callable<Path>() {
      @Override
      public Path call() throws Exception {
        final Path file = Files.createTempFile("file-dump-", ".tmp");
        Files.write(file, content);
        return file;
      }
    });
  }

  private final Callable<Path> creator;
  private Path root;

  public TransientPath(final Callable<Path> creator) {
    this.creator = creator;
  }

  public TransientPath init() {
    assert root == null : "already initialized - call again during test run?";
    Unchecked.run(new Unchecked.Action() {
      @Override
      public void call() throws Exception {
        root = creator.call();
      }
    });
    return this;
  }

  /**
   * the transient path, either a file or directory
   */
  public Path path() {
    assert root != null : "not initialized - called outside of test context?";
    return root;
  }

  /**
   * create a file inside the transient folder, sub folders are created as needed
   */
  public Path add(final Path relativeFileName, final byte[] content) {
    assert root != null : "not initialized - called outside of test context?";
    assert !relativeFileName.isAbsolute() : "name of temporary file must be relative";
    assert Files.isDirectory(root) : "TransientPath is not a directory";
    final Path file = root.resolve(relativeFileName);
    Unchecked.run(new Unchecked.Action() {
      @Override
      public void call() throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
      }
    });
    return file;
  }

  @Override
  public void close() {
    if (root != null) {
      Unchecked.run(new Unchecked.Action() {
        @Override
        public void call() throws Exception {
          Files.walkFileTree(root, ClearFolder.instance());
        }
      });
    }
  }

  // === junit rule implementation =================================================================

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try (final AutoCloseable ignored = TransientPath.this.init()) {
          base.evaluate();
        }
      }
    };
  }

  // === directory cleaner =========================================================================


  /**
   * Attempt to delete all contents in the walked path and the path itself.
   */
  private static final class ClearFolder extends SimpleFileVisitor<Path> {
    private ClearFolder() {
    }

    private static ClearFolder instance() {
      return new ClearFolder();
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
      Files.delete(file); // even if file attributes cannot be read, maybe it can be removed ?
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
