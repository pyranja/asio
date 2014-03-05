package at.ac.univie.isc.asio.jena;

import java.util.Locale;
import java.util.Set;

import org.openjena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.coordination.EngineSpec;
import at.ac.univie.isc.asio.coordination.Operator;
import at.ac.univie.isc.asio.coordination.OperatorCallback;
import at.ac.univie.isc.asio.security.VphToken;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.CSVOutput;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * Execute SPARQL queries through jena's ARQ.
 * 
 * @author Chris Borckholder
 */
public class JenaEngine implements DatasetEngine, Operator {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(JenaEngine.class);

  private final ListeningExecutorService exec;
  private final Model model;

  private final String modelPrefixes;

  public JenaEngine(final ListeningExecutorService exec, final Model model) {
    super();
    this.exec = exec;
    this.model = model;
    modelPrefixes = createPrefixFromModel(model);
  }

  private String createPrefixFromModel(final Model model) {
    final StringBuilder prefixIntro = new StringBuilder();
    for (final String prefix : model.getNsPrefixMap().keySet()) {
      prefixIntro.append(String.format(Locale.ENGLISH, "PREFIX %s: <%s>%n", prefix,
          model.getNsPrefixURI(prefix)));
    }
    return prefixIntro.toString();
  }

  /**
   * return all {@link JenaFormats formats} supported by Jena
   */
  @Override
  public Set<SerializationFormat> supports() {
    return JenaFormats.asSet();
  }

  @Override
  public Type type() {
    return EngineSpec.Type.SPARQL;
  }

  @Override
  public void invoke(final DatasetOperation operation, final Transfer exchange,
      final OperatorCallback callback) {
    // assert parameters and parse query
    final JenaFormats format = failIfInvalidFormat(operation.format());
    final Query query = QueryFactory.create(attachPrefixes(operation.commandOrFail()));
    log.trace("-- parsed ARQ query :\n{}", query);
    // parameterize and invoke the query worker
    final QueryModeHandler<?> handler = forQuery(query, format);
    final SparqlRunner<?> task = SparqlRunner.create(exchange, callback, handler);
    // prepare execution
    final QueryExecution invocation = QueryExecutionFactory.create(query, model);
    delegateCredentials(operation, invocation);
    log.debug(">> invoking ARQ query");
    final ListenableFuture<Void> future = exec.submit(task.use(invocation));
    Futures.addCallback(future, callbackFor(operation, callback));
    log.debug("<< ARQ query invoked");
  }

  private static final Symbol KEY_AUTH_USERNAME = Symbol
      .create("http://jena.hpl.hp.com/Service#queryAuthUser");
  private static final Symbol KEY_AUTH_PASSWORD = Symbol
      .create("http://jena.hpl.hp.com/Service#queryAuthPwd");

  private void delegateCredentials(final DatasetOperation operation, final QueryExecution invocation) {
    if (operation.owner() instanceof VphToken) {
      final VphToken token = (VphToken) operation.owner();
      log.debug("found vph token - delegating credentials from {}", token);
      invocation.getContext().set(KEY_AUTH_USERNAME, "");
      invocation.getContext().set(KEY_AUTH_PASSWORD, String.valueOf(token.getToken()));
      token.destroy();
    } else {
      log.debug("no vph token found - skipping credentials delegation");
    }
  }

  private QueryModeHandler<?> forQuery(final Query query, final JenaFormats format) {
    switch (query.getQueryType()) {
      case Query.QueryTypeSelect:
        return new SelectHandler(formatterFor(format));
      case Query.QueryTypeAsk:
        return new AskHandler(formatterFor(format));
      case Query.QueryTypeConstruct:
        return new ConstructHandler(languageFor(format));
      case Query.QueryTypeDescribe:
        return new DescribeHandler(languageFor(format));
      default:
        throw new AssertionError("unsupported query type");
    }
  }

  private Lang languageFor(final JenaFormats format) {
    switch (format) {
      case XML:
        return Lang.RDFXML;
      default:
        throw new DatasetUsageException(format + " not supported for this query");
    }
  }

  private OutputFormatter formatterFor(final JenaFormats format) {
    switch (format) {
      case SPARQL_XML:
      case XML:
        return new XMLOutput();
      case CSV:
        return new CSVOutput();
      default:
        throw new DatasetUsageException(format + " not supported for this query");
    }
  }

  private String attachPrefixes(final String query) {
    return modelPrefixes + query;
  }

  private FutureCallback<Void> callbackFor(final DatasetOperation operation,
      final OperatorCallback handler) {
    return new FutureCallback<Void>() {
      @Override
      public void onSuccess(final Void result) {
        // *no-op*
      }

      @Override
      public void onFailure(final Throwable t) {
        final DatasetFailureException error = new DatasetFailureException(t);
        error.setFailedOperation(operation);
        handler.fail(error);
      }
    };
  }

  private JenaFormats failIfInvalidFormat(final SerializationFormat given) {
    if (given instanceof JenaFormats) {
      return (JenaFormats) given;
    } else {
      final String message = String.format(Locale.ENGLISH, "unexpected format %s", given);
      throw new DatasetFailureException(message, new ClassCastException());
    }
  }
}
