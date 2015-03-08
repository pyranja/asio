package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Capture information on an error, e.g. during request processing.
 *
 * @see <a href='https://github.com/blongden/vnd.error'>vnd.error</a>
 */
@AutoValue
public abstract class Error {
  /** Registered error media type name */
  public static final String MEDIA_TYPE_NAME = "application/vnd.error+json";

  /**
   * Create an error descriptor from a java exception. Ensures that either the exception message or
   * if not available the exception class name are included as human readable message.
   *
   * @param exception the exception that occurred
   * @param correlation an identifier of the failed activity
   * @param timestamp time when the error occurred
   * @param includeTrace whether the full stack trace of the exception is included
   * @return initialized error
   */
  public static Error from(final Throwable exception, final Correlation correlation, final long timestamp, final boolean includeTrace) {
    final Throwable root = Throwables.getRootCause(exception);
    final String message = exception.getMessage() == null
        ? exception.getClass().getSimpleName()
        : exception.getMessage();
    final ImmutableList<StackTraceElement> trace = includeTrace
        ? ImmutableList.copyOf(exception.getStackTrace())
        : ImmutableList.<StackTraceElement>of();
    return create(message, root.toString(), correlation, timestamp, trace);
  }

  /**
   * internal use only!
   */
  @JsonCreator
  static Error create(@JsonProperty("message") final String message,
                      @JsonProperty("cause") final String cause,
                      @JsonProperty("logref") final Correlation correlation,
                      @JsonProperty("timestamp") final long timestamp,
                      @JsonProperty("trace") final List<StackTraceElement> trace) {
    return new AutoValue_Error(message, cause, correlation, timestamp, ImmutableList.copyOf(trace));
  }

  Error() { /* prevent sub classes */ }

  /**
   * Short description of the error and its cause if known.
   */
  @JsonProperty("message")
  public abstract String getMessage();

  /**
   * String representation of the root exception, as given by {@link Throwable#toString()}.
   */
  @JsonProperty("cause")
  public abstract String getCause();

  /**
   * An id that allows to relate the error to an activity, e.g. a single request.
   */
  @JsonProperty("logref")
  public abstract Correlation getCorrelation();

  /**
   * When the error was recorded.
   */
  @JsonProperty("timestamp")
  public abstract long getTimestamp();

  /**
   * If enabled, the complete stack trace of the occurred exception.
   */
  @JsonProperty("trace")
  public abstract List<StackTraceElement> getStackTrace();
}
