package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.base.Charsets;
import com.google.common.base.Ticker;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Publish events to a guava event bus.
 */
@Component
public final class EventBusEmitter implements Emitter {
  private static final Logger log = getLogger(EventBusEmitter.class);

  public static final Correlation SYSTEM_CORRELATION;
  static {
    final String name = "urn:asio:system-correlation";
    final UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(Charsets.UTF_8));
    SYSTEM_CORRELATION = Correlation.valueOf(uuid.toString());
    log.info(Scope.SYSTEM.marker(), "using system correlation <{}>", SYSTEM_CORRELATION);
  }

  /**
   * Create an emitter, which uses the given time source and correlation id.
   */
  public static EventBusEmitter create(final EventBus bus,
                                       final Ticker time,
                                       final Correlation correlation) {
    return new EventBusEmitter(bus, time, new Provider<Correlation>() {
      @Override
      public Correlation get() {
        return correlation;
      }
    });
  }

  private final EventBus bus;
  private final Ticker time;
  private final Provider<Correlation> correlationProvider;

  @Autowired
  EventBusEmitter(final EventBus bus, final Ticker time, Provider<Correlation> correlationProvider) {
    this.bus = bus;
    this.time = time;
    this.correlationProvider = correlationProvider;
  }

  @PostConstruct
  public void reportInitialization() {
    log.info(Scope.SYSTEM.marker(), "eventbus-emitter active - resolved system correlation to {}", correlation());
  }

  @Override
  public Event emit(final Event event) {
    event.init(correlation(), time.read());
    bus.post(event);
    return event;
  }

  @Override
  public VndError emit(final Throwable exception) {
    final VndError error = VndError.from(exception, correlation(), time.read(), true);
    final ErrorEvent event = new ErrorEvent(error);
    emit(event);
    return error;
  }

  /**
   * Attempt to resolve a possibly scoped correlation from the provide. Fall back to system
   * correlation if resolving fails (e.g. not in a request context).
   */
  private Correlation correlation() {
    try {
      final Correlation scoped = correlationProvider.get();
      log.trace("found scoped correlation - {}", scoped);
      return scoped;
    } catch (final BeanCreationException e) {
      log.trace("no scoped correlation found - falling back to system");
      return SYSTEM_CORRELATION;
    } catch (final Exception e) {
      log.warn("unexpected error when resolving scoped correlation : {}", e.toString());
      return SYSTEM_CORRELATION;
    }
  }

  /** Wrap an Error and adapt it to the event interface */
  private static final class ErrorEvent extends Event {
    private final VndError error;

    private ErrorEvent(final VndError error) {
      super("error", "error");
      this.error = error;
    }

    @JsonUnwrapped
    @JsonIgnoreProperties("timestamp")  // prevent duplication of timestamp attribute
    public VndError getError() {
      return error;
    }
  }
}
