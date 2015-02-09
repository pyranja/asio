package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import com.google.auto.value.AutoValue;

import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.Map;

/**
 * Captures an asio event.
 */
@AutoValue
@Immutable
public abstract class Event implements ServerSentEvent {
  static Event create(final Correlation correlation, final long timestamp, final Scope scope, final Message message) {
    return new AutoValue_Event(correlation, timestamp, scope, message);
  }

  // auto-value properties

  /**
   * Related event will share a correlation id.
   * @return correlation id of this event
   */
  public abstract Correlation correlation();

  /**
   * Wall clock time, when this event was recorded.
   * @return creation time as milliseconds since epoch
   */
  public abstract long timestamp();

  /**
   * The scope in which this event occurred, e.g. in the context of a single request.
   * @return event scope
   */
  public abstract Scope scope();

  /**
   * @return internal container of event context
   */
  public abstract Message message();

  // end of auto-value properties

  // SSE implementation

  /**
   * This uses the event's {@link #scope()} as event type.
   * @return event type as specified by SSE spec
   */
  @Override
  public String type() {
    return scope().toString();
  }

  /**
   * A JSON representation of the event.
   * @return event data as specified by SSE spec
   */
  @Override
  public String data() {
    final StringWriter sink = new StringWriter();
    final JsonGenerator json = Json.createGenerator(sink);
    json.writeStartObject()
        .write("correlation", correlation().toString())
        .write("timestamp", timestamp())
        .write("scope", scope().toString())
        .write("subject", message().subject())
        ;
    json.writeStartObject("context");
    for (Map.Entry<String, String> each : message().content().entrySet()) {
      json.write(each.getKey(), each.getValue());
    }
    json.writeEnd().writeEnd().flush();
    return sink.toString();
  }
}
