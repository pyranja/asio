package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.insight.Event;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Create and emit {@link at.ac.univie.isc.asio.insight.Event request events}.
 */
public final class EventReporter {
  static final String REQUEST_TYPE = "request";
  // standard messages
  static final String ACCEPTED = String.valueOf("accepted");
  static final String RECEIVED = String.valueOf("received");
  static final String EXECUTED = String.valueOf("executed");
  static final String COMPLETED = String.valueOf("completed");
  static final String REJECTED = String.valueOf("rejected");
  static final String FAILED = String.valueOf("failed");

  public static final Joiner COMMA_SEPARATED = Joiner.on(",");

  private final EventBus bus;
  private final Ticker time;
  private final Event.Correlation correlation;

  public EventReporter(final EventBus bus, final Ticker time) {
    this.bus = bus;
    this.time = time;
    final UUID uuid = UUID.randomUUID();
    correlation = Event.Correlation.valueOf(uuid.toString());
  }

  Event build(final String message, final Map<String, String> context) {
    final Event event = Event.make(REQUEST_TYPE)
        .correlation(correlation)
        .timestamp(time.read())
        .context(context)
        .create(message);
    bus.post(event);
    return event;
  }

  public Event event(final String message) {
    return build(message, ImmutableMap.<String, String>of());
  }

  // fluent interface
  public ContextHolder with(final Parameters parameters) {
    return new ContextHolder()
        .and(parameters.properties().asMap())
        .and("accepted", parameters.acceptable());
  }

  public ContextHolder with(final Throwable error) {
    //noinspection ThrowableResultOfMethodCallIgnored
    return new ContextHolder()
        .and("message", Objects.toString(error.getMessage())) // null safe
        .and("cause", error.toString())
        .and("root", Throwables.getRootCause(error).toString())
        .and("trace", Throwables.getStackTraceAsString(error)); // FIXME : switch with debug flag
  }

  public ContextHolder with(final Map<String, ? extends Iterable<?>> properties) {
    return new ContextHolder().and(properties);
  }

  /** fluent interface helper */
  final class ContextHolder {
    private final ImmutableMap.Builder<String, String> context;

    ContextHolder() {
      this.context = ImmutableMap.builder();
    }

    /** finish building
     * @param message*/
    public Event event(final String message) {
      return build(message, context.build());
    }

    public ContextHolder and(final Map<String, ? extends Iterable<?>> multiMap) {
      for (Map.Entry<String, ? extends Iterable<?>> each : multiMap.entrySet()) {
        context.put(each.getKey(), COMMA_SEPARATED.join(each.getValue()));
      }
      return this;
    }

    public ContextHolder and(final String key, final String value) {
      context.put(key, value);
      return this;
    }

    public ContextHolder and(final String key, final Iterable<?> values) {
      context.put(key, COMMA_SEPARATED.join(values));
      return this;
    }
  }
}
