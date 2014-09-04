package at.ac.univie.isc.asio.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

/**
 * Utility methods for resource handling and clean up.
 */
public final class Resources {
  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(Resources.class);

  private static final String ERROR_MSG = "error while cleaning up {} : {}";

  /**
   * Close the given {@link AutoCloseable resource} if it is not null. If an exception occurs while
   * closing, it is logged with level WARN, but not rethrown.
   *
   * @param that to be closed
   */
  public static void close(final AutoCloseable that) {
    if (that != null) {
      try {
        that.close();
      } catch (final Exception e) {
        log.warn(ERROR_MSG, that, e.getMessage(), e);
      }
    } else {
      log.warn(ERROR_MSG, that, "was null");
    }
  }

  /**
   * Close as if it is an {@code AutoCloseable}.
   * @see #close(AutoCloseable)
   * @param response to be closed
   */
  public static void close(Response response) {
    if (response != null) {
      try {
        response.close();
      } catch (final Exception e) {
        log.warn(ERROR_MSG, response, e.getMessage(), e);
      }
    } else {
      log.warn(ERROR_MSG, response, "was null");
    }
  }

  /**
   * Close as if it is an {@code AutoCloseable}.
   * @see #close(AutoCloseable)
   * @param xmlWriter to be closed
   */
  public static void close(final XMLStreamWriter xmlWriter) {
    if (xmlWriter != null) {
      try {
        xmlWriter.close();
      } catch (final Exception e) {
        log.warn(ERROR_MSG, xmlWriter, e.getMessage(), e);
      }
    } else {
      log.warn(ERROR_MSG, xmlWriter, "was null");
    }
  }

  private static final Class<?> CLIENT_DISCONNECT_EXCEPTION;
  static class ClientDisconnectUnknown { /** dummy class */ }
  static {  // find the exception class indicating a client disconnect if running in a container
    Class<?> holder;
    try {
      holder = Class.forName("org.apache.catalina.connector.ClientAbortException");
    } catch (ClassNotFoundException e) {
      holder = Resources.ClientDisconnectUnknown.class;
    }
    CLIENT_DISCONNECT_EXCEPTION = holder;
    log.info("[BOOT] using {} as client disconnect indicator", CLIENT_DISCONNECT_EXCEPTION.getName());
  }

  /**
   * Inspect the given exception and determine, whether it is caused by a client closing its
   * connection to the web server. A best effort is made to detect the hosting servlet container.
   * @param exception to be inspected
   * @return true if the given exception is caused by a disconnected client
   */
  public static boolean indicatesClientDisconnect(@Nonnull final IOException exception) {
    return CLIENT_DISCONNECT_EXCEPTION.isAssignableFrom(exception.getClass());
  }

  private Resources() {/* static helper */}
}
