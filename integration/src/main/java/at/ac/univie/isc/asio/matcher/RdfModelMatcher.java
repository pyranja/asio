package at.ac.univie.isc.asio.matcher;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.openjena.riot.Lang;

import java.io.StringReader;

class RdfModelMatcher extends TypeSafeMatcher<String> {
  private final Model expected;
  private final Lang format;

  RdfModelMatcher(final Model expected, final Lang format) {
    this.expected = expected;
    this.format = format;
  }

  @Override
  protected boolean matchesSafely(final String item) {
    final Model actual = ModelFactory.createDefaultModel();
    actual.read(new StringReader(item), null, format.getName());
    return expected.isIsomorphicWith(actual);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(expected);
  }
}
