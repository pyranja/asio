/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.platform;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.ConfigStore;
import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.FindFiles;
import at.ac.univie.isc.asio.tool.Pretty;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import static com.google.common.io.Files.asByteSource;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Store configuration files in the file system,
 * using {@link java.nio.file.Path file paths} as references to the configuration items.
 * The store expects to have exclusive control over the given {@code root} directory,
 * concurrent modifications have unpredictable consequences.
 * <p>
 * <strong>Note:</strong> Individual method calls are synchronized internally to prevent file
 * corruption. If atomicity is required for composed operations, external synchronization has to be
 * provided.
 * </p>
 */
public final class FileSystemConfigStore implements ConfigStore {
  private static final Logger log = getLogger(FileSystemConfigStore.class);

  /**
   * Thrown if configuration files cannot be read from or written to the file system.
   */
  public static final class FileSystemAccessFailure extends DataAccessResourceFailureException implements AsioError {
    public FileSystemAccessFailure(final String msg, final Throwable cause) {
      super(msg, cause);
    }
  }

  /** The sub-folder of the working directory used to store configuration files. */
  public static final Path STORE_FOLDER = Paths.get("brood");

  private final Path root;
  private final Timeout timeout;

  private final Lock lock;

  /**
   * @param root base working directory
   * @param timeout maximum time allowed to acquire internal lock
   */
  public FileSystemConfigStore(final Path root, final Timeout timeout) {
    this.timeout = timeout;
    log.info(Scope.SYSTEM.marker(), "initializing in <{}>", root);
    lock = new ReentrantLock();
    try {
      this.root = Files.createDirectories(root.resolve(STORE_FOLDER)).toAbsolutePath();
      touch();
    } catch (IOException cause) {
      throw new FileSystemAccessFailure("cannot create configuration store folder", cause);
    }
  }

  @VisibleForTesting
  Path getRoot() {
    return root;
  }

  /**
   * Create a marker file in the working directory. Serves as a fail-fast mechanism to detect
   * filesystem access problems.
   *
   * @throws IOException if the marker file cannot be written
   */
  private void touch() throws IOException {
    final List<String> content =
        Collections.singletonList(Long.toString(System.currentTimeMillis()));
    Files.write(this.root.resolve(".asio"), content, Charsets.UTF_8);
  }

  @Override
  public Map<String, ByteSource> findAllWithIdentifier(final String identifier) throws DataAccessException {
    final FindFiles collector = FindFiles.filter(filesWithIdentifier(identifier));
    Map<String, ByteSource> found = new HashMap<>();
    try {
      log.debug(Scope.SYSTEM.marker(), "searching items with identifier <{}>", identifier);
      lock();
      Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, collector);
      for (final Path path : collector.found()) {
        log.debug(Scope.SYSTEM.marker(), "found <{}>", path);
        final Path fileName = path.getFileName();
        final String[] parts = fileName.toString().split("\\#\\#");
        found.put(parts[0], asByteSource(path.toFile()));
      }
    } catch (IOException e) {
      throw new FileSystemAccessFailure("finding items of type <" + identifier + "> failed", e);
    } finally {
      lock.unlock();
    }
    return ImmutableMap.copyOf(found);
  }

  @Override
  public URI save(final String qualifier, final String identifier, final ByteSource content) {
    requireNonNull(content, "file content");
    final Path file = resolve(qualifier, identifier);
    try {
      log.debug(Scope.SYSTEM.marker(), "saving item as <{}>", file);
      lock();
      try (final OutputStream sink = Files.newOutputStream(file)) {
        content.copyTo(sink);
      }
      return file.toUri();
    } catch (IOException e) {
      throw new FileSystemAccessFailure("failed to save to <" + file + ">", e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void clear(final String qualifier) {
    final FindFiles collector = FindFiles.filter(filesWithQualifier(qualifier));
    try {
      log.debug(Scope.SYSTEM.marker(), "clearing configuration of <{}>", qualifier);
      lock();
      Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, collector);
      for (final Path path : collector.found()) {
        log.debug(Scope.SYSTEM.marker(), "deleting <{}>", path);
        Files.deleteIfExists(path);
      }
    } catch (IOException e) {
      throw new FileSystemAccessFailure("failed to clear <" + qualifier + ">", e);
    } finally {
      lock.unlock();
    }
  }

  private void lock() {
    try {
      if (!lock.tryLock(timeout.getAs(TimeUnit.MILLISECONDS, 0L), TimeUnit.MILLISECONDS)) {
        throw new CannotAcquireLockException("failed to acquire store lock in " + timeout);
      }
    } catch (InterruptedException e) {
      throw new CannotAcquireLockException("interrupted while acquiring config store lock", e);
    }
  }

  // MUST keep qualifier - identifier separator in sync in globs and resolver

  private PathMatcher filesWithIdentifier(final String identifier) {
    final String glob = Pretty.format("glob:**/*##%s", validate(identifier));
    return FindFiles.matchOnlyRegularFilesAnd(root.getFileSystem().getPathMatcher(glob));
  }

  private PathMatcher filesWithQualifier(final String qualifier) {
    final String glob = Pretty.format("glob:**/%s##*", validate(qualifier));
    return FindFiles.matchOnlyRegularFilesAnd(root.getFileSystem().getPathMatcher(glob));
  }

  private Path resolve(final String qualifier, final String name) {
    final String file = validate(qualifier) + "##" + validate(name);
    return root.resolve(file);
  }

  /**
   * Allow only simple characters (a-z, A-Z, 0-9, _, - and .) in names.
   */
  static final Pattern LEGAL_IDENTIFIER = Pattern.compile("^[\\w][\\w\\.-]+$");

  private String validate(final String raw) {
    if (raw == null || !LEGAL_IDENTIFIER.matcher(raw).matches()) {
      throw new IllegalConfigStoreLabel(raw);
    }
    return raw;
  }

  static final class IllegalConfigStoreLabel extends InvalidUsage {
    public IllegalConfigStoreLabel(final String label) {
      super(Pretty.format("illegal characters in label <%s> - allowed are [a-z, A-Z, 0-9, _, ., -]", label));
    }
  }

  @Override
  public String toString() {
    return "FileSystemConfigStore{" +
        "root=" + root +
        ", timeout=" + timeout +
        '}';
  }
}
