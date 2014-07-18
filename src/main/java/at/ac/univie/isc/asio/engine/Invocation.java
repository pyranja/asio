package at.ac.univie.isc.asio.engine;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.security.Role;

/**
 * Wrap a two-phase operation on a dataset.
 * <p>
 * An invocation has to be {@link #execute() executed} first,
 * then results can be {@link #write(java.io.OutputStream) written} to a sink exactly once.
 * </p>
 */
public interface Invocation extends AutoCloseable {

  /**
   * @return role required to execute this.
   */
  Role requires();

  /**
   * Perform the operation and prepare resources for result serialization.
   *
   * @throws java.lang.IllegalStateException        if this has already been {@link #execute() executed}
   * @throws at.ac.univie.isc.asio.DatasetException on any error
   */
  void execute() throws DatasetException;

  /**
   * @return the MIME type of the serialized results.
   */
  MediaType produces();

  /**
   * Write serialized results to the given {@code sink}.
   *
   * @param sink an {@link java.io.OutputStream}
   * @throws java.io.IOException                    if writing to the given {@code sink} fails.
   * @throws at.ac.univie.isc.asio.DatasetException on any internal error
   */
  void write(OutputStream sink) throws IOException, DatasetException;

  /**
   * Interrupt this invocation, if it is active. Concurrent {@link #execute()} or
   * {@link #write(java.io.OutputStream)} may fail due to interruption.
   *
   * @throws at.ac.univie.isc.asio.DatasetException on any internal error
   */
  void cancel() throws DatasetException;

  /**
   * Free all resources associated with this invocation. If the invocation is {@code active} it may
   * be interrupted. Closing an already closed {@code Invocation} has no effect.
   *
   * @throws at.ac.univie.isc.asio.DatasetException on any internal error
   */
  @Override
  void close() throws DatasetException;
}
