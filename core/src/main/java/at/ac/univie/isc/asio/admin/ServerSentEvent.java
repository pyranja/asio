package at.ac.univie.isc.asio.admin;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static java.util.Objects.requireNonNull;

/**
 * Element of an event-stream.
 */
interface ServerSentEvent {
  /**
   * @return type of the event
   */
  String type();

  /**
   * @return payload of the event
   */
  String data();

  /**
   * Write {@code server-sent-events} to a wrapped {@code OutputStream}.
   *
   * @see <a href="http://www.w3.org/TR/eventsource/">SSE W3C recommendation</a>
   */
  @NotThreadSafe
  final class Writer implements AutoCloseable, Flushable {
    /** IANA registered media type for event streams */
    public static final String EVENT_STREAM_MIME = "text/event-stream";
    /** Character encoding of event streams (always UTF-8) */
    public static final Charset EVENT_STREAM_CHARSET = Charset.forName("UTF-8");

    /**
     * @param sink the stream to write to
     * @return the writer
     */
    public static Writer wrap(@Nonnull final OutputStream sink) {
      return new Writer(sink);
    }

    private final java.io.Writer stream;

    private Writer(@Nonnull final OutputStream sink) {
      requireNonNull(sink);
      this.stream = new OutputStreamWriter(sink, EVENT_STREAM_CHARSET);
    }

    /**
     * @param message of the comment
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer comment(@Nonnull final String message) throws IOException {
      stream.append(':').append(message).append('\n');
      return this;
    }

    /**
     * @param payload of current event
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer data(@Nonnull final String payload) throws IOException {
      stream.append("data:").append(payload).append('\n');
      return this;
    }

    /**
     * @param type of current event
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer event(@Nonnull final String type) throws IOException {
      stream.append("event:").append(type).append('\n');
      return this;
    }

    /**
     * @param id of current event
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer id(@Nonnull final String id) throws IOException {
      stream.append("id:").append(id).append('\n');
      return this;
    }

    /**
     * @param delay for client reconnection attempts
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer retryAfter(final int delay) throws IOException {
      assert delay >= 0 : "illegal retry delay (" + delay + ")";
      stream.append("retry:").append(Integer.toString(delay)).append('\n');
      return this;
    }

    /**
     * Ends the current event.
     * @return the writer
     * @throws java.io.IOException on any error
     */
    public Writer boundary() throws IOException {
      stream.append('\n');
      return this;
    }

    @Override
    public void flush() throws IOException {
      stream.flush();
    }

    /**
     * Close the wrapped {@code OutputStream}.
     * @throws java.io.IOException if closing fails
     */
    @Override
    public void close() throws IOException {
      stream.close();
    }
  }


  @Immutable
  final class Simple implements ServerSentEvent {
    public static Simple create(final String type, final String data) {
      return new Simple(type, data);
    }

    private final String type;
    private final String data;

    private Simple(final String type, final String data) {
      this.type = type;
      this.data = data;
    }

    @Override
    public String type() {
      return type;
    }

    @Override
    public String data() {
      return data;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final Simple that = (Simple) o;

      if (data != null ? !data.equals(that.data) : that.data != null) {
        return false;
      }
      if (type != null ? !type.equals(that.type) : that.type != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (data != null ? data.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("type", type)
          .add("data", data)
          .toString();
    }
  }
}
