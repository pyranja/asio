package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.admin.Event;
import at.ac.univie.isc.asio.admin.ServerSentEvent;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;

import java.util.Map;
import java.util.UUID;

/**
 * Create and emit {@link at.ac.univie.isc.asio.admin.Event request events}.
 */
public final class EventReporter {
  static final ServerSentEvent.Type REQUEST_TYPE = ServerSentEvent.Type.valueOf("request");
  // standard messages
  static final Event.Message ACCEPTED = Event.Message.valueOf("accepted");
  static final Event.Message RECEIVED = Event.Message.valueOf("received");
  static final Event.Message EXECUTED = Event.Message.valueOf("executed");
  static final Event.Message COMPLETED = Event.Message.valueOf("completed");
  static final Event.Message REJECTED = Event.Message.valueOf("rejected");
  static final Event.Message FAILED = Event.Message.valueOf("failed");

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

  Event build(final Event.Message message, final Map<String, String> context) {
    final Event event = Event.make(REQUEST_TYPE)
        .correlation(correlation)
        .timestamp(time.read())
        .context(context)
        .create(message);
    bus.post(event);
    return event;
  }

  public Event event(final Event.Message message) {
    return build(message, ImmutableMap.<String, String>of());
  }

  // fluent interface
  public ContextHolder with(final Parameters parameters) {
    return new ContextHolder()
        .and(parameters.properties().asMap())
        .and("accepted", parameters.acceptable());
  }

  public ContextHolder with(final Command command) {
    return new ContextHolder()
        .and(command.properties().asMap());
  }

  public ContextHolder with(final Throwable error) {
    //noinspection ThrowableResultOfMethodCallIgnored
    return new ContextHolder()
        .and("message", error.getMessage())
        .and("cause", error.toString())
        .and("root", Throwables.getRootCause(error).toString())
        .and("trace", Throwables.getStackTraceAsString(error)); // FIXME : switch with debug flag
  }

  /** fluent interface helper */
  final class ContextHolder {
    private final ImmutableMap.Builder<String, String> context;

    ContextHolder() {
      this.context = ImmutableMap.builder();
    }

    /** finish building */
    public Event event(final Event.Message message) {
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
