package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.tool.TypedValue;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Immutable
public final class Event implements ServerSentEvent {
  private final Type type;
  private final Message message;
  private final Correlation correlation;
  private final long timestamp;
  private final Map<String, String> context;

  Event(final Type type, final Message message, final Correlation correlation, final long timestamp, final Map<String, String> context) {
    this.type = requireNonNull(type);
    this.correlation = requireNonNull(correlation);
    this.message = requireNonNull(message);
    this.timestamp = requireNonNull(timestamp);
    this.context = requireNonNull(context);
  }

  @Nonnull
  @Override
  public Type type() {
    return type;
  }

  public Correlation correlation() {
    return correlation;
  }

  public Message message() {
    return message;
  }

  public long timestamp() {
    return timestamp;
  }

  public Map<String, String> context() {
    return context;
  }

  @Override
  public void writeTo(@Nonnull final Writer sink) throws IOException {
    final JsonGenerator json = Json.createGenerator(sink);
    json.writeStartObject()
        .write("message", message.toString())
        .write("correlation", correlation.toString())
        .write("timestamp", timestamp);
    json.writeStartObject("context");
    for (Map.Entry<String, String> each : context.entrySet()) {
      json.write(each.getKey(), each.getValue());
    }
    json.writeEnd();
    json.writeEnd();
    json.flush();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(type.toString())
        .add("message", message)
        .add("correlation", correlation)
        .add("timestamp", timestamp)
        .add("context", context)
        .toString();
  }

  public static class Message extends TypedValue<String> {
    public static Message valueOf(final String val) {
      return new Message(val);
    }

    private Message(final String val) {
      super(val);
    }

    @Nonnull
    @Override
    protected String normalize(@Nonnull final String val) {
      return val.toLowerCase(Locale.ENGLISH);
    }
  }

  public static class Correlation extends TypedValue<String> {
    public static Correlation valueOf(final String val) {
      return new Correlation(val);
    }

    private Correlation(final String val) {
      super(val);
    }
  }

  public static Builder make(final Type type) {
    return new Builder(type);
  }

  public static class Builder {
    private final Type type;
    private Correlation correlation;
    private long timestamp;
    private Map<String, String> context = Collections.emptyMap();

    public Builder(final Type type) {
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

    public Event create(final Message message) {
      return new Event(type, message, correlation, timestamp, context);
    }
  }
}
