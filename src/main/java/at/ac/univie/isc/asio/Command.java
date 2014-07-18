package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.Role;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

public interface Command {

  MediaType format();

  Role requiredRole();

  Observable<Results> observe();

  public interface Results extends StreamingOutput, AutoCloseable {
    /**
     * Write the serialized results to the supplied {@link java.io.OutputStream}.
     *
     * @param output the OutputStream to write to.
     * @throws java.io.IOException if an IO error is encountered
     * @throws at.ac.univie.isc.asio.DatasetException if an internal error occurs
     * // @throws IllegalStateException if results already consumed //
     */
    @Override
    void write(OutputStream output) throws IOException, DatasetException;

    /**
     * Release resources associated with the results, for example an open database cursor. Closing
     * <strong>may</strong> discard not yet consumed result data and abort an active serialization.
     *
     * @throws at.ac.univie.isc.asio.DatasetException if this resource cannot be closed
     */
    @Override
    void close() throws DatasetException;
  }
}
