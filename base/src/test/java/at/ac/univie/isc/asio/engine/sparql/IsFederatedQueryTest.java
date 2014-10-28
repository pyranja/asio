package at.ac.univie.isc.asio.engine.sparql;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class IsFederatedQueryTest {

  private Predicate<Query> subject;

  @Before
  public void setUp() throws Exception {
    subject = new IsFederatedQuery();
  }

  @Test
  public void recognizes_pure_federated_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { SERVICE <http://example.com> { ?s ?p ?o }}");
    assertThat(subject.apply(query), is(true));
  }

  @Test
  public void recognizes_pure_non_federated_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { ?s ?p ?o}");
    assertThat(subject.apply(query), is(false));
  }

  @Test
  public void recognizes_nested_service_in_mixed_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { ?s ?p ?o . SERVICE <http://example.com> { ?s ?p ?o } }");
    assertThat(subject.apply(query), is(true));
  }

  @Test
  public void recognizes_empty_pattern() throws Exception {
    final Query query = QueryFactory.create("SELECT * WHERE {}");
    assertThat(subject.apply(query), is(false));
  }

  @Test
  public void query_without_pattern_is_ignored() throws Exception {
    final Query query = QueryFactory.create("DESCRIBE <http://example.com>");
    assertThat(subject.apply(query), is(false));
  }
}
