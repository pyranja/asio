package at.ac.univie.isc.asio.admin;

import at.ac.univie.isc.asio.tool.TypedValue;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * An event, which can be published through a Server-Sent-Event stream.
 */
public interface ServerSentEvent {
  /** Magic {@link #type()} value representing an event without custom type */
  public static final Type GENERIC = Type.valueOf("generic");
  /** Magic {@link #type()} value representing a comment line (these are ignored by clients) */
  public static final Type COMMENT = Type.valueOf("comment");

  /**
   * The custom name of this event's type or the constant {@link #GENERIC} if it is a generic event.
   * @return the event type name.
   */
  @Nonnull
  Type type();

  /**
   * Serialize this event and write it to the given event stream.
   * @param sink the event stream output
   * @throws IOException on any error while writing
   */
  void writeTo(@Nonnull Writer sink) throws IOException;

  public static class Type extends TypedValue<String> {
    public static Type valueOf(final String val) {
      return new Type(val);
    }

    private Type(final String val) {
      super(val);
    }

    @Nonnull
    @Override
    protected String normalize(@Nonnull final String val) {
      return val.toUpperCase(Locale.ENGLISH);
    }
  }

  /**
   * Basic implementation of a SSE.
   */
  @Immutable
  public static final class Default implements ServerSentEvent {
    public static ServerSentEvent create(final Type type, final String payload) {
      return new Default(type, payload);
    }

    private final Type type;
    private final String payload;

    private Default(final Type type, final String payload) {
      this.type = requireNonNull(type);
      this.payload = requireNonNull(payload);
    }

    @Nonnull
    @Override
    public Type type() {
      return type;
    }

    @Override
    public void writeTo(@Nonnull final Writer sink) throws IOException {
      sink.write(payload);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("type", type)
          .add("payload", payload)
          .toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final Default aDefault = (Default) o;

      if (!payload.equals(aDefault.payload)) {
        return false;
      }
      if (!type.equals(aDefault.type)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + payload.hashCode();
      return result;
    }
  }
}
