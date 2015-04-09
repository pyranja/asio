package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.security.Identity;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for events related to protocol operations.
 */
public final class Operation {
  /**
   * empty property map
   */
  private static final Map<String, Object> NO_PROPERTIES = Collections.emptyMap();

  /**
   * Triggered immediately after parsing an operation request.
   */
  public static Event received(final Command command) {
    final Map<String, Object> properties = new HashMap<>();
    for (Map.Entry<String, ? extends Iterable<?>> each : command.properties().asMap().entrySet()) {
      if (Event.VALID_PROPERTY_NAME.apply(each.getKey())) {
        properties.put(each.getKey(), each.getValue());
      }
    }
    properties.put("accepted", command.acceptable());
    properties.put("owner", command.owner().or(Identity.undefined()));
    return new OperationEvent("received", properties);
  }

  /**
   * Triggered by successful execution of an operation.
   */
  public static Event executed() {
    return new OperationEvent("executed", NO_PROPERTIES);
  }

  /**
   * Triggered after operation results have been streamed completely.
   */
  public static Event completed() {
    return new OperationEvent("completed", NO_PROPERTIES);
  }

  /**
   * Triggered if an operation fails at any stage.
   */
  public static Event failure(final Throwable cause) {
    final Map<String, String> properties =
        Collections.singletonMap("message", VndError.labelFor(cause));
    return new OperationEvent("failed", properties);
  }

  /**
   * Allows to add dynamic properties.
   */
  private static class OperationEvent extends Event {
    private final Map<String, ?> properties;

    protected OperationEvent(final String subject, final Map<String, ?> properties) {
      super("operation", subject);
      this.properties = ImmutableMap.copyOf(properties);
    }

    @JsonAnyGetter
    public Map<String, ?> getProperties() {
      return properties;
    }
  }
}
