package at.ac.univie.isc.asio.junit;

import com.hp.hpl.jena.rdf.model.Model;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

public final class IsIsomorphic extends TypeSafeMatcher<Model> {

  /**
   * Create a matcher for {@link com.hp.hpl.jena.rdf.model.Model jena rdf models}, matching when the
   * examined model is {@link com.hp.hpl.jena.rdf.model.Model#isIsomorphicWith(com.hp.hpl.jena.rdf.model.Model) isomorphic with}
   * (in other words 'equal to') the given reference model.
   *
   * @param reference expected RDF model
   */
  @Factory
  public static Matcher<Model> isomorphicWith(final Model reference) {
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
