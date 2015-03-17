package at.ac.univie.isc.asio.io;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * hamcrest matchers for JDK7 {@link java.nio.file.Path paths}.
 */
public final class PathMatchers {
  private PathMatchers() {}

  public static Matcher<? super Path> aDirectory() {
    return new PathMatcher("an existing directory") {
      @Override
      protected boolean check(final Path item, final Description mismatch) throws IOException {
        if (!Files.exists(item)) {
          mismatch.appendText(" does not exist");
          return false;
        }
        if (!Files.isDirectory(item)) {
          mismatch.appendText(" is not a directory");
          return false;
        }
        return true;
      }
    };
  }

  public static Matcher<? super Path> aFile() {
    return new PathMatcher("an existing file") {
      @Override
      protected boolean check(final Path item, final Description mismatch) throws IOException {
        if (!Files.exists(item)) {
          mismatch.appendText(" does not exist");
          return false;
        }
        if (!Files.isRegularFile(item)) {
          mismatch.appendText(" is not a file");
          return false;
        }
        return true;
      }
    };
  }

  static abstract class PathMatcher extends TypeSafeDiagnosingMatcher<Path> {
    private final String expectation;

    protected PathMatcher(final String expectation) {
      this.expectation = expectation;
    }

    protected abstract boolean check(final Path item, final Description mismatch) throws IOException;

    @Override
    protected final boolean matchesSafely(final Path item, final Description mismatch) {
      mismatch.appendValue(item);
      try {
        return check(item, mismatch);
      } catch (Exception e) {
        mismatch.appendText(" checking failed <").appendValue(e).appendText(">");
        return false;
      }
    }

    @Override
    public final void describeTo(final Description description) {
      description.appendText(expectation);
    }
  }
}
