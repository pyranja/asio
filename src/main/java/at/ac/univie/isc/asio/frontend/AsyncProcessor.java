package at.ac.univie.isc.asio.frontend;

import at.ac.univie.isc.asio.Result;
import com.google.common.util.concurrent.ListenableFuture;

import javax.ws.rs.container.AsyncResponse;

/**
 * @author pyranja
 */
public interface AsyncProcessor {
  /**
   * Resume the given {@link javax.ws.rs.container.AsyncResponse} when the given {@link at.ac.univie.isc.asio.Result} future completes.
   *
   * @param future pending results
   * @param response to be continued
   */
  void handle(ListenableFuture<Result> future, AsyncResponse response);
}
