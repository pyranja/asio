package at.ac.univie.isc.asio.tool;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;

/**
 * Determines whether a MediaType is compatible with some reference type.
 */
public class CompatibleTo extends TypeSafeMatcher<MediaType> {

  @Factory
  public static <T> Matcher<MediaType> compatibleTo(final MediaType reference) {
    return new CompatibleTo(reference);
  }

  private final MediaType reference;

  public CompatibleTo(final MediaType reference) {
    this.reference = requireNonNull(reference);
  }

  @Override
  protected boolean matchesSafely(final MediaType item) {
    return reference.isCompatible(item);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(reference);
  }
}
