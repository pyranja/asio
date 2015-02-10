package at.ac.univie.isc.asio.web;

import at.ac.univie.isc.asio.web.HttpCode;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static java.util.Objects.requireNonNull;

public final class HttpMatchers {
  private HttpMatchers() {}

  /**
   * Creates a matcher for HTTP status codes matching if the given status code belongs to the
   * expected {@link at.ac.univie.isc.asio.web.HttpCode family} of HTTP status codes.
   * @param expected family of HTTP status codes
   */
  @Factory
  public static Matcher<? super Integer> indicates(final HttpCode expected) {
    return new HttpStatusFamilyMatcher(expected);
  }

  private static class HttpStatusFamilyMatcher extends TypeSafeMatcher<Integer> {
    private final HttpCode expected;

    private HttpStatusFamilyMatcher(final HttpCode expected) {
      this.expected = requireNonNull(expected);
    }

    @Override
    protected boolean matchesSafely(final Integer actual) {
      return expected.includes(actual);
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(expected.toString());
    }
  }
}
