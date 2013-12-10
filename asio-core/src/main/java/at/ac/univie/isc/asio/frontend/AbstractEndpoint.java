package at.ac.univie.isc.asio.frontend;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.common.LogContext;
import at.ac.univie.isc.asio.coordination.EngineSpec;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Generic request processing for DatasetEndpoints.
 * 
 * @author Chris Borckholder
 */
public class AbstractEndpoint {

  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(AbstractEndpoint.class);

  // XXX make configurable
  private static final long TIMEOUT = 10L; // SECONDS

  // dependencies
  private final EngineSelector registry;
  private final AsyncProcessor processor;
  private final EngineSpec.Type type;
  protected final OperationFactory create;

  /**
   * subclass constructor
   * 
   * @param engine adapted backing dataset
   * @param processor completes requests asynchronously
   * @param create operation factory
   * @param type of concrete endpoint
   */
  protected AbstractEndpoint(final EngineSelector registry, final AsyncProcessor processor,
      final OperationFactory create, final EngineSpec.Type type) {
    super();
    this.registry = registry;
    this.processor = processor;
    this.create = create;
    this.type = type;
  }

  /**
   * Complete the operation information and execute it.
   * 
   * @param partial dataset operation
   * @param request JAXRS request
   * @param response JAXRS suspended response
   */
  protected final void complete(final OperationBuilder partial, final Request request,
      final AsyncResponse response) {
    try {
      final EngineAdapter engine = registry.select(type).get();
      final DatasetOperation operation = engine.completeWithMatchingFormat(request, partial);
      MDC.put(LogContext.KEY_OP, operation.toString());
      log.info(">> executing operation");
      final ListenableFuture<Result> future = engine.submit(operation);
      log.debug("<< operation submitted");
      response.setTimeout(TIMEOUT, TimeUnit.SECONDS);
      processor.handle(future, response);
    } catch (final Throwable t) {
      log.warn("error on request completion", t);
      response.resume(t); // resume immediately on error
    } finally {
      MDC.clear(); // thread will return to container pool
    }
  }
}
