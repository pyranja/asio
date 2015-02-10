package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import com.google.common.base.Ticker;
import com.google.common.eventbus.EventBus;

import java.util.UUID;

/**
 * Enrich {@code Messages} with context information and post them to an
 * {@link com.google.common.eventbus.EventBus}.
 */
public final class EventBusEmitter implements Emitter {
  private final EventBus bus;
  private final Ticker time;
  private final Correlation correlation;
  private final Scope scope;

  public EventBusEmitter(final EventBus bus, final Ticker time, final Scope scope) {
    this.bus = bus;
    this.time = time;
    this.scope = scope;
    correlation = Correlation.valueOf(UUID.randomUUID().toString());
  }

  public static EventBusEmitter create(final EventBus bus, final Ticker time, final Scope scope) {
    return new EventBusEmitter(bus, time, scope);
  }

  @Override
  public void emit(final Message message) {
    bus.post(Event.create(correlation, time.read(), scope, message));
  }
}
