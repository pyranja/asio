package org.hamcrest.text;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

/**
 * Match strings against regular expressions.
 *
 * Ported from future hamcrest : https://github.com/hamcrest/JavaHamcrest/blob/master/hamcrest-library/src/main/java/org/hamcrest/text/MatchesPattern.java
 */
public final class MatchesPattern extends TypeSafeMatcher<String> {
  /**
   * Create a {@code String} matcher, comparing items to the given regular expression.
   *
   * @param pattern compiled regular expression
   */
  public static Matcher<String> matchesPattern(Pattern pattern) {
    return new MatchesPattern(pattern);
  }

  /**
   * Create a {@code String} matcher, comparing items to the given regular expression.
   *
   * @param regex regular expression
   */
  public static Matcher<String> matchesPattern(String regex) {
    return new MatchesPattern(Pattern.compile(regex));
  }

  private final Pattern pattern;

  public MatchesPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  protected boolean matchesSafely(String item) {
    return pattern.matcher(item).matches();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a string matching the pattern '" + pattern + "'");
  }
}
