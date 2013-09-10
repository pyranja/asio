package at.ac.univie.isc.asio.jena;

import java.security.Principal;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.ResultHandler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Execute SPARQL queries through jena's ARQ.
 * 
 * @author Chris Borckholder
 */
public class JenaEngine implements DatasetEngine {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(JenaEngine.class);

  private final ListeningExecutorService exec;
  private final Model model;

  private final String modelPrefixes;

  JenaEngine(final ListeningExecutorService exec, final Model model) {
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
  public Set<SerializationFormat> supportedFormats() {
    return JenaFormats.asSet();
  }

  @Override
  public void submit(final DatasetOperation operation, final ResultHandler handler,
      final Principal ignored) {
    // assert parameters and parse query
    final JenaFormats format = failIfInvalidFormat(operation.format());
    final Query query = QueryFactory.create(attachPrefixes(operation.commandOrFail()));
    log.trace("-- parsed ARQ query :\n{}", query);
    // parameterize and invoke the query worker
    final Callable<Void> task = new QueryTask(handler, format, query, model);
    log.debug(">> invoking ARQ query");
    final ListenableFuture<Void> future = exec.submit(task);
    Futures.addCallback(future, callbackFor(operation, handler));
    log.debug("<< ARQ query invoked");
  }

  private String attachPrefixes(final String query) {
    return modelPrefixes + query;
  }

  private FutureCallback<Void> callbackFor(final DatasetOperation operation,
      final ResultHandler handler) {
    return new FutureCallback<Void>() {
      @Override
      public void onSuccess(final Void result) {
        handler.complete();
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
