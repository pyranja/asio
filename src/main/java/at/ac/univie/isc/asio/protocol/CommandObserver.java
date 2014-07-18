package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Command;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * Bridge an observable stream to an {@link javax.ws.rs.container.AsyncResponse}
 */
class CommandObserver extends Subscriber<Command.Results> {
  private static final Logger log = LoggerFactory.getLogger(CommandObserver.class);

  public static CommandObserver bridgeTo(final AsyncResponse async) {
    return new CommandObserver(async);
  }

  private final AsyncResponse async;

  @VisibleForTesting
  CommandObserver(final AsyncResponse async) {
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
      async.resume(e);
    } else {
      log.warn("must swallow error - response already resumed", e);
    }
  }

  @Override
  public void onNext(final Command.Results results) {
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
