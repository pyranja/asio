package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.DatasetOperation;
import rx.Observable;

/**
 * Execute given operations - possibly asynchronous - and provide result data as {@link rx.Observable}.
 *
 * @author pyranja
 */
public interface Engine {

  /**
   * Verify and execute the command given by the {@code operation}. Execution may be asynchronous,
   * but results and execution state are always transferred through the retruned {@code Observable}.
   * {@link rx.Subscription#unsubscribe() Unsubscribing} <strong>may</strong> cancel a running
   * execution.
   * <p>
   * Errors occurring before or during execution, are emitted by the returned {@code Observable}.
   * </p>
   *
   * @param operation describe the action to be taken, metadata and authorization
   * @return Nested {@code Observable}. The outer {@code Observable} will emit a single inner
   * {@code Observable} as soon as streaming of result data may begin.
   * Once subscribed, the inner one will emit byte[] chunks until all result data has been processed.
   */
  Observable<Command.Results> execute(DatasetOperation operation);
}
