package at.ac.univie.isc.asio.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamWriter;

import static java.util.Objects.requireNonNull;

/**
 * An action, which should close its input object.
 *
 * @see AutoCloseable
 * @param <T> type of closed object
 */
public abstract class Closer<T> {
  final static Logger log = LoggerFactory.getLogger(Closer.class);
  /**
   * Close the given, non-null object.
   *
   * @param it the object that should be closed
   * @throws Exception any error during closing it
   */
  public abstract void close(@Nonnull T it) throws Exception;

  // === helper to quietly close objects ===========================================================

  private static final String ERROR_MSG = "error while cleaning up {} : {}";

  /**
   * Close the given {@link AutoCloseable resource} if it is not null. If an exception occurs while
   * closing, it is logged with level WARN, but not rethrown.
   *
   * @param that to be closed
   */
  public static void quietly(@Nullable final AutoCloseable that) {
    quietly(that, autoCloseable());
  }

  /**
   * Quietly close the given object, using the given action. Any exception during execution of the
   * closing action is caught and logged, but not rethrown, except if it is an {@link Error}.
   *
   * @param it the object that should be closed
   * @param closer action that will close the instance
   * @param <T> type of closed object
   */
  public static <T> void quietly(@Nullable final T it, @Nonnull final Closer<T> closer) {
    requireNonNull(closer, "missing closer action");
    if (it != null) {
      try {
        closer.close(it);
      } catch (Exception e) {
        log.warn(ERROR_MSG, it, e.getMessage(), e);
      }
    } else {
      log.warn(ERROR_MSG, "<unknown>", "was null");
    }
  }

  public static AutoCloseableCloser autoCloseable() {
    return AUTO_CLOSEABLE_CLOSER;
  }

  public static Closer<XMLStreamWriter> xmlStreamWriter() {
    return XML_STREAM_WRITER_CLOSER;
  }

  private static final AutoCloseableCloser AUTO_CLOSEABLE_CLOSER = new AutoCloseableCloser();

  private static final XmlStreamWriterCloser XML_STREAM_WRITER_CLOSER = new XmlStreamWriterCloser();

  private static class AutoCloseableCloser extends Closer<AutoCloseable> {
    @Override
    public void close(@Nonnull final AutoCloseable it) throws Exception {
      it.close();
    }
  }

  private static class XmlStreamWriterCloser extends Closer<XMLStreamWriter> {
    @Override
    public void close(@Nonnull final XMLStreamWriter it) throws Exception {
      it.close();
    }
  }
}
