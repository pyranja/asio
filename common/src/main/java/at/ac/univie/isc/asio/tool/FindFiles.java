/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * A {@code FileVisitor} that collects all paths of visited files, which match the given
 * {@code PathMatcher}. This visitor may only be used once.
 */
public final class FindFiles extends SimpleFileVisitor<Path> {
  /**
   * Include only files, which match the given condition.
   *
   * @param condition a path matcher
   * @return finding visitor
   */
  public static FindFiles filter(final PathMatcher condition) {
    return new FindFiles(condition);
  }

  /**
   * Initialize a visitor that includes any visited file.
   *
   * @return a visitor that collects any encountered file
   */
  public static FindFiles any() {
    return new FindFiles(new Any());
  }

  /**
   * Create a composite path matcher with the given condition. The result will match only if the
   * input is a {@link Files#isRegularFile(Path, LinkOption...) regular files} <strong>and</strong>
   * matches the supplied delegate condition.
   *
   * @param condition other match condition
   * @return composite matcher
   */
  public static PathMatcher matchOnlyRegularFilesAnd(final PathMatcher condition) {
    return new RestrictToRegularFiles(condition);
  }

  private final PathMatcher condition;
  private final ImmutableList.Builder<Path> found;

  private FindFiles(final PathMatcher condition) {
    found = ImmutableList.builder();
    this.condition = condition;
  }

  /**
   * All files that were visited and matched the {@code PathMatcher}.
   *
   * @return collection of all visited files in order
   */
  public List<Path> found() {
    return found.build();
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    if (condition.matches(file)) {
      found.add(file);
    }
    return FileVisitResult.CONTINUE;
  }

  /** path matcher that delegates to another but excludes anything that is not a regular file */
  private static class RestrictToRegularFiles implements PathMatcher {
    private final PathMatcher condition;

    public RestrictToRegularFiles(final PathMatcher condition) {
      this.condition = condition;
    }

    @Override
    public boolean matches(final Path path) {
      return Files.isRegularFile(path) && condition.matches(path);
    }
  }

  /** path matcher that always returns true */
  private static class Any implements PathMatcher {
    @Override
    public boolean matches(final Path path) {
      return true;
    }
  }
}
