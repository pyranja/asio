package at.ac.univie.isc.asio.matcher;

import com.google.common.net.MediaType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Locale;

final class MimeMatcher extends TypeSafeMatcher<String> {
  private static final String WILDCARD = "*";

  private final MediaType expected;

  public MimeMatcher(final MediaType expected) {
    this.expected = expected;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(expected);
  }

  @Override
  protected boolean matchesSafely(final String item) {
    final MediaType actual = MediaType.parse(item.toLowerCase(Locale.ENGLISH));
    return mainTypeCompatible(actual) && subTypeCompatible(actual) && parametersCompatible(actual);
  }

  private boolean parametersCompatible(final MediaType actual) {
    return actual.parameters().entries().containsAll(expected.parameters().entries());
  }

  private boolean subTypeCompatible(final MediaType actual) {
    return (expected.subtype().equals(WILDCARD) || actual.subtype().equals(expected.subtype()) || actual.subtype().endsWith("+" + expected.subtype()));
  }

  private boolean mainTypeCompatible(final MediaType actual) {
    return expected.type().equals(WILDCARD) || actual.type().equals(expected.type());
  }
}
