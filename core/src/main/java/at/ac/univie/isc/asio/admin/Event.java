package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.tool.TypedValue;
import com.google.common.base.Objects;

import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Immutable
public final class Event implements ServerSentEvent {
  private final String type;
  private final String message;
  private final Correlation correlation;
  private final long timestamp;
  private final Map<String, String> context;

  Event(final String type, final String message, final Correlation correlation, final long timestamp, final Map<String, String> context) {
    this.type = requireNonNull(type);
    this.correlation = requireNonNull(correlation);
    this.message = requireNonNull(message);
    this.timestamp = requireNonNull(timestamp);
    this.context = requireNonNull(context);
  }

  public Correlation correlation() {
    return correlation;
  }

  public long timestamp() {
    return timestamp;
  }

  @Override
  public String type() {
    return type;
  }

  public String message() {
    return message;
  }

  public Map<String, String> context() {
    return context;
  }

  @Override
  public String data() {
    final StringWriter sink = new StringWriter();
    final JsonGenerator json = Json.createGenerator(sink);
    json.writeStartObject()
        .write("type", type)
        .write("message", message)
        .write("correlation", correlation.toString())
        .write("timestamp", timestamp);
    json.writeStartObject("context");
    for (Map.Entry<String, String> each : context.entrySet()) {
      json.write(each.getKey(), each.getValue());
    }
    json.writeEnd().writeEnd().flush();
    return sink.toString();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", type)
        .add("message", message)
        .add("correlation", correlation)
        .add("timestamp", timestamp)
        .add("context", context)
        .toString();
  }

  public static class Correlation extends TypedValue<String> {
    public static Correlation valueOf(final String val) {
      return new Correlation(val);
    }

    private Correlation(final String val) {
      super(val);
    }
  }

  public static Builder make(final String type) {
    return new Builder(type);
  }

  public static class Builder {
    private final String type;
    private Correlation correlation;
    private long timestamp;

    private Map<String, String> context = Collections.emptyMap();

    public Builder(final String type) {
      this.type = type;
    }

    public Builder correlation(final Correlation correlation) {
      this.correlation = correlation;
      return this;
    }

    public Builder timestamp(final long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder context(final Map<String, String> context) {
      this.context = context;
      return this;
    }

    public Event create(final String message) {
      return new Event(type, message, correlation, timestamp, context);
    }
  }
}
