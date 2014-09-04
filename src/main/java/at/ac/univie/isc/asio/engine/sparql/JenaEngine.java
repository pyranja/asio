package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Parameters;
import at.ac.univie.isc.asio.security.Token;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.mgt.Explain;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public final class JenaEngine implements Engine {
  private static final Logger log = LoggerFactory.getLogger(JenaEngine.class);

  public static final String KEY_QUERY = "query";
  public static final Symbol CONTEXT_AUTH_USERNAME =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthUser");
  public static final Symbol CONTEXT_AUTH_PASSWORD =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthPwd");

  private final Model model;
  private final TimeoutSpec timeout;
  private final HandlerFactory handlers;

  public JenaEngine(final Model model, final TimeoutSpec timeout) {
    this.model = requireNonNull(model);
    this.timeout = requireNonNull(timeout);
    handlers = new HandlerFactory();
  }

  @Override
  public Language language() {
    return Language.SPARQL;
  }

  @Override
  public SparqlInvocation<?> prepare(final Parameters params, final Principal owner) {
    // must set model's default prefixes before parsing
    final Query query = QueryFactory.create();
    query.getPrefixMapping().withDefaultMappings(model);
    QueryFactory.parse(query, params.require(KEY_QUERY), null, Syntax.syntaxARQ);
    log.debug("parsed ARQ query\n{}", query);
    final SparqlInvocation<?> handler = handlers.select(query.getQueryType(), params.acceptable());
    final QueryExecution execution = QueryExecutionFactory.create(query, model);
    execution.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL);  // FIXME disable || parameterize || set global context value
    execution.setTimeout(timeout.getAs(TimeUnit.MILLISECONDS, -1L));
    attachCredentials(execution, owner);
    handler.init(execution);
    log.debug("using handler {}", handler);
    return handler;
  }

  private void attachCredentials(final QueryExecution execution, final Principal owner) {
    if (owner instanceof Token && owner != Token.ANONYMOUS) {
      log.debug("delegating credentials from {}", owner);
      final Token token = (Token) owner;
      execution.getContext().set(CONTEXT_AUTH_USERNAME, "");
      execution.getContext().set(CONTEXT_AUTH_PASSWORD, token.getToken());
    } else {
      log.debug("skipping credentials delegation - not a valid auth token {}", owner);
    }
  }

  public static final class UnknownQueryType extends DatasetUsageException {
    public UnknownQueryType() {
      super("unknown SPARQL query type");
    }
  }
}
