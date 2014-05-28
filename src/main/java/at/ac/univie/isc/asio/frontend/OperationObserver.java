package at.ac.univie.isc.asio.frontend;

import at.ac.univie.isc.asio.transport.ObservableStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * Bridge an observable stream to an {@link javax.ws.rs.container.AsyncResponse}
 *
 * @author pyranja
 */
public class OperationObserver extends Subscriber<ObservableStream> {
  private static final Logger log = LoggerFactory.getLogger(OperationObserver.class);

  private final AsyncResponse async;
  private final Response.ResponseBuilder response;

  public OperationObserver(final AsyncResponse async, final Response.ResponseBuilder response) {
    this.async = async;
    this.response = response;
  }

  @Override
  public void onCompleted() {
    if (async.isSuspended()) {
      log.warn("got no data from operation - sending no content response");
      // FIXME resume with no content exception
      async.resume(response.status(Response.Status.NO_CONTENT).variant(null).build());
    }
  }

  @Override
  public void onError(final Throwable e) {
    if (async.isSuspended()) {
      async.resume(e);
    } else {
      log.warn("must swallow error - response already resumed", e);
    }
  }

  @Override
  public void onNext(final ObservableStream observableStream) {
    if (async.isSuspended()) {
      log.debug("resuming response on thread {}", Thread.currentThread());
      async.resume(response.status(Response.Status.OK).entity(observableStream).build());
    } else {
      log.warn("cannot send results - response already resumed");
      unsubscribe();
    }
  }
}
