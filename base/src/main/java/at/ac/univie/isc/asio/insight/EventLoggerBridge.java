package at.ac.univie.isc.asio.insight;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Log {@link ServerSentEvent server} and
 * {@link com.google.common.eventbus.DeadEvent unhandled} events
 * to a {@link #LOGGER_NAME SLF4J logger}.
 */
public class EventLoggerBridge {
  /** special logger name for events */
  public static final String LOGGER_NAME = "at.ac.univie.isc.asio.events";
  private static final Logger eventLog = LoggerFactory.getLogger(LOGGER_NAME);
  private static final Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");

  @Subscribe
  public void log(final Event event) {
    eventLog.info(EVENT_MARKER, "{}", event);
  }

  @Subscribe
  public void logDeadEvent(final DeadEvent dead) {
    eventLog.warn(EVENT_MARKER, "DEAD : {}", dead);
  }
}
