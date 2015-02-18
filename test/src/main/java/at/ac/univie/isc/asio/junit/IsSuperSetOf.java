package at.ac.univie.isc.asio.junit;

import com.hp.hpl.jena.rdf.model.Model;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class IsSuperSetOf extends TypeSafeMatcher<Model> {
  /**
   * Create a matcher for {@link com.hp.hpl.jena.rdf.model.Model jena rdf models}, matching when the
   * examined model contains all statements in the given reference model.
   *
   * @param expected expected RDF statements
   */
  public static IsSuperSetOf superSetOf(final Model expected) {
    return new IsSuperSetOf(expected);
  }

  private final Model expected;

  private IsSuperSetOf(final Model expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(final Model item) {
    return item.containsAll(expected);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("model containing all statements from ").appendValue(expected);
  }
}
