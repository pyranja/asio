package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.base.Ticker;
import com.google.common.eventbus.EventBus;

/**
 * Publish events to a guava event bus.
 */
public final class EventBusEmitter implements Emitter {

  /**
   * Create an emitter, which uses the given time source and correlation id.
   */
  public static EventBusEmitter create(final EventBus bus,
                                       final Ticker time,
                                       final Correlation correlation) {
    return new EventBusEmitter(bus, time, correlation);
  }

  private final EventBus bus;
  private final Ticker time;
  private final Correlation correlation;

  EventBusEmitter(final EventBus bus, final Ticker time, Correlation correlation) {
    this.bus = bus;
    this.time = time;
    this.correlation = correlation;
  }

  @Override
  public Event emit(final Event event) {
    event.init(correlation, time.read());
    bus.post(event);
    return event;
  }

  @Override
  public VndError emit(final Throwable exception) {
    final VndError error = VndError.from(exception, correlation, time.read(), true);
    final ErrorEvent event = new ErrorEvent(error);
    emit(event);
    return error;
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
