package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap {@link at.ac.univie.isc.asio.engine.Invocation} results and provide them as stream entity.
 */
public abstract class StreamedResults implements StreamingOutput, AutoCloseable {
  private final Subject<Void, Void> progress = BehaviorSubject.create();
  private final MediaType format;

  protected StreamedResults(final MediaType format) {
    this.format = format;
  }

  /**
   * Called to write the result stream.
   *
   * @param output the OutputStream to write to.
   * @throws java.io.IOException if an IO error is encountered
   */
  protected abstract void doWrite(final OutputStream output) throws IOException;

  /**
   * Write the serialized results to the supplied {@link java.io.OutputStream}.
   *
   * @param output the OutputStream to write to.
   * @throws java.io.IOException                    if an IO error is encountered
   * @throws at.ac.univie.isc.asio.DatasetException if an internal error occurs
   *                                                // @throws IllegalStateException if results already consumed //
   */
  @Override
  public final void write(final OutputStream output) throws IOException, DatasetException {
    try {
      doWrite(output);
      progress.onCompleted();
    } catch (final Throwable cause) {
      progress.onError(cause);
      throw cause;
    }
  }

  /**
   * Release resources associated with the results, for example an open database cursor. Closing
   * <strong>may</strong> discard not yet consumed result data and abort an active serialization.
   *
   * The default is a no-op, implementations should override this method.
   *
   * @throws at.ac.univie.isc.asio.DatasetException if this resource cannot be closed
   */
  @Override
  public void close() throws DatasetException {};

  /**
   * Empty observable, which propagates errors during streaming and completes when all results were
   * streamed.
   *
   * @return observable progress of result stream
   */
  public final Observable<Void> progress() {
    return progress;
  }

  /**
   * @return the MIME type of this serialized result.
   */
  public final MediaType format() {
    return format;
  }
}
