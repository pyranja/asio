package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Language;
import at.ac.univie.isc.asio.engine.Parameters;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public final class JenaEngine implements Engine {
  private static final Logger log = LoggerFactory.getLogger(JenaEngine.class);

  public static final String KEY_QUERY = "query";

  public static JenaEngine create(final Model model, final TimeoutSpec timeout, final boolean allowFederated) {
    return new JenaEngine(new DefaultJenaFactory(model, timeout, allowFederated));
  }

  private final HandlerFactory handlers;
  private final DefaultJenaFactory state;

  private JenaEngine(final DefaultJenaFactory state) {
    this.state = state;
    handlers = new HandlerFactory();
  }

  @Override
  public Language language() {
    return Language.SPARQL;
  }

  @Override
  public SparqlInvocation<?> prepare(final Parameters params, final Principal owner) {
    final Query query = state.parse(params.require(KEY_QUERY));
    log.debug("parsed ARQ query\n{}", query);
    final SparqlInvocation<?> handler = handlers.select(query.getQueryType(), params.acceptable());
    final QueryExecution execution = state.execution(query, owner);
    handler.init(execution);
    log.debug("using handler {}", handler);
    return handler;
  }

  @Override
  public void close() {
    this.state.close();
  }

  public static final class UnknownQueryType extends DatasetUsageException {
    public UnknownQueryType() {
      super("unknown SPARQL query type");
    }
  }

  public static final class FederatedQueryLocked extends DatasetUsageException {
    public FederatedQueryLocked() {
      super("execution of federated SPARQL queries is disabled");
    }
  }
}
