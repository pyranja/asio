package at.ac.univie.isc.asio.transport;

import at.ac.univie.isc.asio.DatasetTransportException;
import rx.functions.Action2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static java.util.Objects.requireNonNull;

/**
 * Serialize {@code ObservableStreams}.
 *
 * @see at.ac.univie.isc.asio.transport.ObservableStream
 */
@Provider
public class ObservableStreamBodyWriter implements MessageBodyWriter<ObservableStream> {

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
                             final Annotation[] annotations,
                             final MediaType mediaType) {
    return type != null && ObservableStream.class.isAssignableFrom(type);
  }

  /**
   * {@inheritDoc} <p>
   * This implementation will <strong>always</strong> report an unknown content length.
   * All parameters are ignored.
   * @return {@code -1} always
   */
  @Override
  public long getSize(final ObservableStream observableStream, final Class<?> type,
                      final Type genericType,
                      final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  /**
   * {@inheritDoc} <p>
   * A subscriber is added to the given {@code ObservableStream}, which writes emitted chunks to
   * {@code sink}. Blocks until the observable is terminated.
   * @param source observable sequence of {@code byte[]} chunks
   * @param sink outbound response message stream
   * @throws at.ac.univie.isc.asio.DatasetTransportException on non-fatal errors
   */
  @Override
  public void writeTo(final ObservableStream source, final Class<?> type,
                      final Type genericType, final Annotation[] annotations,
                      final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                      final OutputStream sink)
      throws IOException, WebApplicationException {
    requireNonNull(source, "response entity missing on message write");
    requireNonNull(sink, "response stream missing on message write");
    source.collect(sink, StreamCollector.INSTANCE).toBlocking().single();
  }

  private static class StreamCollector implements Action2<OutputStream, byte[]> {
    public static final StreamCollector INSTANCE = new StreamCollector();

    @Override
    public void call(final OutputStream output, final byte[] bytes) {
      try {
        output.write(bytes);
      } catch (IOException e) {
        throw new DatasetTransportException(e);
      }
    }
  }
}
