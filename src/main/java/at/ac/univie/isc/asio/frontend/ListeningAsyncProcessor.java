package at.ac.univie.isc.asio.frontend;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Await {@link Result} completion and resume the JAXRS response after completion.
 *
 * @author Chris Borckholder
 */
public class ListeningAsyncProcessor implements AsyncProcessor {

  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(ListeningAsyncProcessor.class);

  private final ExecutorService exec;
  private final VariantConverter converter;

  private TimeoutSpec timeout;

  public ListeningAsyncProcessor(final ExecutorService exec, final VariantConverter converter) {
    super();
    this.exec = exec;
    this.converter = converter;
    this.timeout = TimeoutSpec.undefined();
  }

  public AsyncProcessor withTimeout(TimeoutSpec timeout) {
    this.timeout = requireNonNull(timeout);
    return this;
  }

  /**
   * Resume the given {@link AsyncResponse} when the given {@link Result} future completes.
   *
   * @param future pending results
   * @param response to be continued
   */
  @Override
  public void handle(final ListenableFuture<Result> future, final AsyncResponse response) {
    /*
     * This will only register a callback with the given future and return immediately afterwards to
     * allow the request's thread to return to the container pool. The response will be resumed on
     * another thread from the dedicated response handler pool. A copy of the log context is
     * captured here to allow the thread which executes the callback to continue using it. A
     * response filter will clear it from the resumed response.
     */
    response.setTimeout(timeout.getAs(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    response.setTimeoutHandler(new TimeoutHandler() {
      @Override
      public void handleTimeout(final AsyncResponse asyncResponse) {
        future.cancel(true);
        asyncResponse.resume(new DatasetFailureException("execution time limit exceeded", new TimeoutException()));
      }
    });
    final Map<?, ?> logContext = fetchMDC();
    final FutureCallback<Result> callback = new FutureCallback<Result>() {
      @Override
      public void onSuccess(final Result result) {
        MDC.setContextMap(logContext);
        try {
          log.info("<< operation completed successfully");
          if (response.isSuspended()) {
            final Response success = successResponse(result);
            response.resume(success);
          } else {
            log.error("!! response already resumed on success - cannot send results");
            result.getInput().close();  // try cleaning up input
          }
        } catch (final IOException e) {
          log.error("!! streaming result data failed", e);
          response.resume(wrapFailure(e));
        }
      }

      @Override
      public void onFailure(final Throwable t) {
        MDC.setContextMap(logContext);
        log.info("<< operation failed", t);
        if (response.isSuspended()) {
          response.resume(wrapFailure(t));
        } else {
          log.warn("!! response already resumed on failure - error swallowed", t);
        }
      }
    };
    Futures.addCallback(future, callback, exec);
  }

  /**
   * @return the MDC of the current thread or an empty map. Never null.
   */
  @Nonnull
  private Map<?, ?> fetchMDC() {
    Map<?, ?> maybeContext = MDC.getCopyOfContextMap();
    if (maybeContext == null) {
      maybeContext = Collections.emptyMap();
    }
    final Map<?, ?> logContext = maybeContext;
    return logContext;
  }

  /**
   * @param result query results as stream
   * @return HTTPResponse with Status 200, appropriate content-type and result stream as body
   * @throws IOException if the result stream is corrupt
   */
  @Nonnull
  private Response successResponse(final Result result) throws IOException {
    final MediaType contentType = converter.asContentType(result.mediaType());
    return Response.ok(result.getInput(), contentType).build();
  }

  /**
   * @param t any throwable
   * @return DatasetException wrapping t or t itself if it is a DatasetException.
   */
  @Nonnull
  private DatasetException wrapFailure(final Throwable t) {
    if (t instanceof DatasetException) {
      return (DatasetException) t;
    } else {
      log.warn("!! encountered unwrapped error on operation failure", t);
      return new DatasetFailureException(t);
    }
  }
}
