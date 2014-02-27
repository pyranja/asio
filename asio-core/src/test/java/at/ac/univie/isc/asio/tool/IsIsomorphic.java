package at.ac.univie.isc.asio.tool;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.hp.hpl.jena.rdf.model.Model;

public class IsIsomorphic extends TypeSafeMatcher<Model> {

  @Factory
  public static <T> Matcher<Model> isomorphicWith(final Model reference) {
    return new IsIsomorphic(reference);
  }

  private final Model other;

  public IsIsomorphic(final Model other) {
    super();
    this.other = Objects.requireNonNull(other, "cannot compare to null model");
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(other);
  }

  @Override
  protected boolean matchesSafely(final Model item) {
    return other.isIsomorphicWith(item);
  }
}
