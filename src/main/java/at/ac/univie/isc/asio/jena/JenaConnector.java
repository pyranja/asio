package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.LanguageConnector;
import at.ac.univie.isc.asio.protocol.Parameters;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.Token;
import at.ac.univie.isc.asio.tool.CommandShortener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.mgt.Explain;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public final class JenaConnector implements LanguageConnector {
  private static final Logger log = LoggerFactory.getLogger(JenaConnector.class);

  public static final String KEY_QUERY = "query";
  public static final Symbol CONTEXT_AUTH_USERNAME =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthUser");
  public static final Symbol CONTEXT_AUTH_PASSWORD =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthPwd");

  private final Model model;
  private final Scheduler scheduler;
  private final TimeoutSpec timeout;

  public JenaConnector(final Model model, final Scheduler scheduler, final TimeoutSpec timeout) {
    this.model = requireNonNull(model);
    this.scheduler = requireNonNull(scheduler);
    this.timeout = requireNonNull(timeout);
  }

  @Override
  public Language language() {
    return Language.SPARQL;
  }

  @Override
  public Command createCommand(final Parameters params, final Principal owner) {
    try {
      // must set model's default prefixes before parsing
      final Query query = QueryFactory.create();
      query.getPrefixMapping().withDefaultMappings(model);
      QueryFactory.parse(query, params.require(KEY_QUERY), null, Syntax.syntaxARQ);
      log.debug("parsed ARQ query\n{}", query);
      final JenaQueryHandler handler = HandlerFactory.select(query.getQueryType(), params.acceptable());
      log.debug("using handler {}", handler);
      final QueryExecution execution = QueryExecutionFactory.create(query, model);
      execution.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL);
      execution.setTimeout(timeout.getAs(TimeUnit.MILLISECONDS));
      attachCredentials(execution, owner);
      final JenaCommand command = new JenaCommand(execution, handler);
      log.info("created {}", command);
      return command;
    } catch (final QueryException e) {
      throw new DatasetUsageException(e);
    }
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


  public static final class NoSupportedFormat extends DatasetUsageException {
    public NoSupportedFormat(final Iterable<MediaType> candidates, final Iterable<MediaType> supported) {
      super("No acceptable format in " + candidates + ". Expected one of "+ supported);
    }
  }


  @VisibleForTesting
  final class JenaCommand implements Command {

    private final QueryExecution execution;
    private final JenaQueryHandler handler;
    private String shortQueryCache;

    public JenaCommand(final QueryExecution execution, final JenaQueryHandler handler) {
      this.execution = execution;
      this.handler = handler;
    }

    @Override
    public MediaType format() {
      return handler.format();
    }

    @Override
    public Role requiredRole() {
      return Role.READ;
    }

    @Override
    public Observable<Results> observe() {
      final OnSubscribeExecuteQuery query = new OnSubscribeExecuteQuery(this.execution, handler);
      return Observable.create(query).subscribeOn(scheduler);
    }

    @VisibleForTesting
    QueryExecution execution() {
      return execution;
    }

    @Override
    public String toString() {
      final String query = cachedShortQuery();
      return Objects.toStringHelper(this)
          .add("format", format())
          .add("query", query)
          .toString();
    }

    private String cachedShortQuery() {
      final String query;
      // race does not matter : shortened version will not change
      if (shortQueryCache == null) {
        query = CommandShortener.INSTANCE.apply(execution.getQuery().serialize());
        shortQueryCache = query;
      } else {
        query = shortQueryCache;
      }
      return query;
    }
  }
}
