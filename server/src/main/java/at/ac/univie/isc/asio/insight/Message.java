package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.engine.Command;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * A message consists of a subject and a map of key-value pairs describing some event.
 */
@Immutable
@AutoValue
public abstract class Message {
  /**
   * Start building a Message.
   *
   * @return message builder
   */
  public static Builder create(final String subject) {
    return new Builder(subject);
  }

  public static Message empty(final String subject) {
    return create(subject, ImmutableMap.<String, String>of());
  }

  static Message create(final String subject, final Map<String, String> content) {
    return new AutoValue_Message(subject, content);
  }

  Message() {};

  /**
   * Title of this message. This should be unique for each category of events.
   *
   * @return the title
   */
  public abstract String subject();

  /**
   * Key-value pairs describing context of an event.
   *
   * @return message contents
   */
  public abstract Map<String, String> content();

  /**
   * Gradually build up an event {@code Message}.
   */
  public static class Builder {
    private static final Joiner COMMA_SEPARATED = Joiner.on(",");

    private final String subject;
    private final ImmutableMap.Builder<String, String> content;

    private Builder(final String subject) {
      this.subject = subject;
      content = ImmutableMap.builder();
    }

    /**
     * Add a description of the given command to this message.
     *
     * @param command command that should be recorded
     * @return message with command information added
     */
    public Message with(final Command command) {
      for (Map.Entry<String, ? extends Iterable<?>> each : command.properties().asMap().entrySet()) {
        content.put(each.getKey(), COMMA_SEPARATED.join(each.getValue()));
      }
      content.put("accepted", COMMA_SEPARATED.join(command.acceptable()));
      return empty();
    }

    /**
     * Fill in this message with a description of given error.
     *
     * @param error error that should be recorded
     * @return message with error information added
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public Message with(final Throwable error) {
      content
          .put("message", Objects.toString(error.getMessage()))
          .put("cause", Objects.toString(error))
          .put("root", Objects.toString(Throwables.getRootCause(error)))
          .put("trace", Throwables.getStackTraceAsString(error));
      return empty();
    }

    /**
     * Finish building without adding content.
     *
     * @return complete Message
     */
    public Message empty() {
      return Message.create(subject, content.build());
    }
  }
}
