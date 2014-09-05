package at.ac.univie.isc.asio.admin;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

/**
 * Log {@link at.ac.univie.isc.asio.admin.ServerSentEvent server} and
 * {@link com.google.common.eventbus.DeadEvent unhandled} events
 * to a {@link #LOGGER_NAME SLF4J logger}.
 */
public class EventLoggerBridge implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(EventLoggerBridge.class);

  /** special logger name for events */
  public static final String LOGGER_NAME = "at.ac.univie.isc.asio.events";
  private static final Logger eventLog = LoggerFactory.getLogger(LOGGER_NAME);

  private final EventBus bus;

  /**
   * Immediately subscribe to the given bus.
   * @param bus emitting events
   */
  public EventLoggerBridge(final EventBus bus) {
    this.bus = bus;
    bus.register(this);
    log.info("[BOOT] registered with event bus");
  }

  @Subscribe
  public void log(final ServerSentEvent event) {
    eventLog.info("[EVENT] {}", event);
  }

  @Subscribe
  public void logDeadEvent(final DeadEvent dead) {
    eventLog.warn("[DEAD] {}", dead);
  }

  /**
   * End subscription with the set {@link com.google.common.eventbus.EventBus bus}.
   */
  @PreDestroy
  @Override
  public void close() {
    bus.unregister(this);
    log.info("[BOOT] unregistered from event bus");
  }
}
