package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;

import java.security.Principal;

/**
 * An engine capable of creating invocations on a dataset for a specific
 * {@link Language}.
 */
public interface Engine extends AutoCloseable, Invoker {

  /**
   * @return the {@link Language query language} supported by this engine.
   */
  Language language();

  /**
   * {@inheritDoc}
   */
  @Override
  Invocation prepare(Parameters parameters);

  /**
   * Dispose used resources. Preparing new {@code Invocations} will not be possible afterwards and
   * currently running executions may fail.
   *
   * @throws at.ac.univie.isc.asio.DatasetException
   */
  @Override
  void close() throws DatasetException;
}
