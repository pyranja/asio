package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.tool.Closer;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import javax.annotation.Nonnull;
import java.security.Principal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class DefaultJenaFactory implements JenaFactory {

  private final Timeout timeout;
  private final Dataset dataset;
  private final Model model;

  public DefaultJenaFactory(final Model model, final Timeout timeout) {
    this.model = model;
    this.dataset = DatasetFactory.create(model);
    this.timeout = timeout;
  }

  @Override
  public Query parse(final String sparql) {
    final Query query = QueryFactory.create();
    query.getPrefixMapping().withDefaultMappings(model);
    QueryFactory.parse(query, sparql, null, Syntax.syntaxARQ);
    return query;
  }

  @Override
  public QueryExecution execution(final Query query, final Principal owner) {
    final QueryExecution execution = QueryExecutionFactory.create(query, dataset);
    execution.setTimeout(timeout.getAs(MILLISECONDS, -1L), MILLISECONDS);
    return execution;
  }

  @Override
  public void close() {
    Closer.quietly(model, new Closer<Model>() {
      @Override
      public void close(@Nonnull final Model it) throws Exception {
        it.close();
      }
    });
  }
}
