package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JenaEngine implements Engine {
  private static final Logger log = LoggerFactory.getLogger(JenaEngine.class);

  public static final String KEY_QUERY = "query";

  public static JenaEngine create(final Model model, final Timeout timeout, final boolean allowFederated) {
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
  public SparqlInvocation<?> prepare(final Command command) {
    final Query query = state.parse(command.require(KEY_QUERY));
    log.debug("parsed ARQ query\n{}", query);
    final SparqlInvocation<?> handler = handlers.select(query.getQueryType(), command.acceptable());
    final QueryExecution execution = state.execution(query, command.owner().or(Identity.undefined()));
    handler.init(execution);
    log.debug("using handler {}", handler);
    return handler;
  }

  @Override
  public void close() {
    this.state.close();
  }

  public static final class UnknownQueryType extends InvalidUsage {
    public UnknownQueryType() {
      super("unknown SPARQL query type");
    }
  }

  public static final class FederatedQueryLocked extends InvalidUsage {
    public FederatedQueryLocked() {
      super("execution of federated SPARQL queries is disabled");
    }
  }
}
