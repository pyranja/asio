package at.ac.univie.isc.asio.engine;

import rx.Observable;

import javax.annotation.Nonnull;

/**
 * Invoke an engine operation and let clients observe the results.
 */
public interface Connector {
  /**
   * Attempt to invoke the operation specified by the given arguments. This method will not throw
   * exceptions, but will instead yield an error through the returned {@code Observable}.
   *
   * @param command requested operation
   * @return An observable sequence of {@code StreamedResults}
   */
  @Nonnull
  Observable<StreamedResults> accept(@Nonnull Command command);
}
