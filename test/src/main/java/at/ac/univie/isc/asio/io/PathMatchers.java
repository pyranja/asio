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
