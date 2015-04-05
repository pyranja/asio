package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.FileVisitResult;
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
    return new FindFiles(new PathMatcher() {
      @Override
      public boolean matches(final Path path) {
        return true;
      }
    });
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
}
