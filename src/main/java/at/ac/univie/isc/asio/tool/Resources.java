package at.ac.univie.isc.asio.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamWriter;

/**
 * Utility methods for resource clean up.
 * 
 * @author Chris Borckholder
 */
public final class Resources {

  private static final String ERROR_MSG = "error while cleaning up {} : {}";

  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(Resources.class);

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

  public static void close(final XMLStreamWriter that) {
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

  private Resources() {/* static helper */}
}
