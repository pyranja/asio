package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;

/**
 * An engine capable of creating invocations on a dataset for a specific
 * {@link Language}.
 */
public interface Engine extends AutoCloseable {

  /**
   * @return the {@link Language query language} supported by this engine.
   */
  Language language();

  /**
   * {@inheritDoc}
   */
  Invocation prepare(Command command);

  /**
   * Dispose used resources. Preparing new {@code Invocations} will not be possible afterwards and
   * currently running executions may fail.
   *
   * @throws at.ac.univie.isc.asio.DatasetException
   */
  @Override
  void close() throws DatasetException;
}
