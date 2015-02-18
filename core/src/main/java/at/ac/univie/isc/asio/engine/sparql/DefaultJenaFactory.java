package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.Resources;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.mgt.Explain;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.slf4j.Logger;

import java.security.Principal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

public final class DefaultJenaFactory implements AutoCloseable {
  private static final Logger log = getLogger(DefaultJenaFactory.class);

  public static final IsFederatedQuery IS_FEDERATED_QUERY = new IsFederatedQuery();

  public static final Symbol CONTEXT_AUTH_USERNAME =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthUser");
  public static final Symbol CONTEXT_AUTH_PASSWORD =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthPwd");

  private final TimeoutSpec timeout;
  private final boolean federationEnabled;
  private final Dataset dataset;
  private final Model model;

  public DefaultJenaFactory(final Model model, final TimeoutSpec timeout, final boolean allowFederated) {
    this.model = model;
    this.dataset = DatasetFactory.create(model);
    this.timeout = timeout;
    this.federationEnabled = allowFederated;
  }

  public Query parse(final String sparql) {
    final Query query = QueryFactory.create();
    query.getPrefixMapping().withDefaultMappings(model);
    QueryFactory.parse(query, sparql, null, Syntax.syntaxARQ);
    failIfFederatedDisabled(query);
    return query;
  }

  private void failIfFederatedDisabled(final Query query) {
    if (!federationEnabled && IS_FEDERATED_QUERY.apply(query)) {
      throw new JenaEngine.FederatedQueryLocked();
    }
  }

  public QueryExecution execution(final Query query, final Principal owner) {
    final Context context = makeContext(owner);
    final QueryExecution execution = QueryExecutionFactory.create(query, dataset);
    execution.getContext().putAll(context);
    execution.setTimeout(timeout.getAs(MILLISECONDS, -1L), MILLISECONDS);
    return execution;
  }

  private Context makeContext(final Principal owner) {
    final Context context = new Context();
    // FIXME disable || parameterize || set global context value
    context.set(ARQ.symLogExec, Explain.InfoLevel.ALL);
    if (owner instanceof Identity && ((Identity) owner).isDefined()) {
      log.debug("delegating credentials from {}", owner);
      final Identity identity = (Identity) owner;
      context.set(CONTEXT_AUTH_USERNAME, "");
      context.set(CONTEXT_AUTH_PASSWORD, identity.getToken());
    } else {
      log.debug("skipping credentials delegation - not a valid auth token {}", owner);
    }
    return context;
  }

  @Override
  public void close() {
    Resources.close(model);
  }
}
