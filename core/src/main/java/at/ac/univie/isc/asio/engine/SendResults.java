package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * Send observed {@link at.ac.univie.isc.asio.engine.StreamedResults} as asynchronous response.
 */
final class SendResults extends Subscriber<StreamedResults> {
  private static final Logger log = LoggerFactory.getLogger(SendResults.class);

  /**
   * Create an instance sending result to the given {@code AsyncResponse}.
   * @param async response continuation
   * @return created subscriber
   */
  public static SendResults to(final AsyncResponse async) {
    return new SendResults(async);
  }

  private final AsyncResponse async;

  private SendResults(final AsyncResponse async) {
    this.async = async;
  }

  @Override
  public void onCompleted() {
    if (async.isSuspended()) {
      log.warn("got no data from operation - sending no content response");
      async.resume(Response.noContent().build());
    }
  }

  @Override
  public void onError(final Throwable e) {
    if (async.isSuspended()) {
      final Throwable wrapped = DatasetException.wrapIfNecessary(e);
      async.resume(wrapped);
    } else {
      log.warn("must swallow error - response already resumed", e);
    }
  }

  @Override
  public void onNext(final StreamedResults results) {
    if (async.isSuspended()) {
      log.debug("resuming response on thread {}", Thread.currentThread());
      final Response response = Response
          .ok()
          .entity(results)
          .type(results.format())
          .build();
      async.resume(response);
    } else {
      log.warn("cannot send results - response already resumed");
      unsubscribe();
    }
  }
}
