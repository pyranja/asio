package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.collect.Multimap;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;

/**
 * Encapsulate an invocation of an {@link at.ac.univie.isc.asio.engine.Engine}.
 */
public interface Command {

  /**
   * @return role required to execute this command.
   */
  Role requiredRole();

  /**
   * @return contextual information
   */
  Multimap<String, String> properties();

  /**
   * Start processing of the encapsulated command asynchronously.
   *
   * @return observable yielding either the results or an error.
   */
  Observable<Results> observe();

  public interface Results extends StreamingOutput, AutoCloseable {
    /**
     * Write the serialized results to the supplied {@link java.io.OutputStream}.
     *
     * @param output the OutputStream to write to.
     * @throws java.io.IOException                    if an IO error is encountered
     * @throws at.ac.univie.isc.asio.DatasetException if an internal error occurs
     *                                                // @throws IllegalStateException if results already consumed //
     */
    @Override
    void write(OutputStream output) throws IOException, DatasetException;

    /**
     * @return the MIME type of this serialized result.
     */
    MediaType format();

    /**
     * Release resources associated with the results, for example an open database cursor. Closing
     * <strong>may</strong> discard not yet consumed result data and abort an active serialization.
     *
     * @throws at.ac.univie.isc.asio.DatasetException if this resource cannot be closed
     */
    @Override
    void close() throws DatasetException;
  }

  public interface Factory {
    /**
     * Attempt to create a command, which captures the given parameters.
     * @param parameters of the request
     * @param owner the entity, who initiated the request
     * @return a compiled command
     */
    Command accept(Parameters parameters, Principal owner);

    /**
     * The factory is not able to create a command for the given {@link Language}.
     */
    final class LanguageNotSupported extends DatasetUsageException {
      public LanguageNotSupported(final Language language) {
        super(language + " is not supported");
      }
    }
  }
}
