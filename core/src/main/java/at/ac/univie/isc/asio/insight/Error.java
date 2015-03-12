package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.tool.Pretty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Capture information on an error, e.g. during request processing.
 *
 * @see <a href='https://github.com/blongden/vnd.error'>vnd.error</a>
 */
@AutoValue
public abstract class Error {
  /**
   * Registered error media type name
   */
  public static final String MEDIA_TYPE_NAME = "application/vnd.error+json";

  /**
   * Create an error descriptor from a java exception. Ensures that either the exception message or
   * if not available the exception class name are included as human readable message.
   *
   * @param exception    the exception that occurred
   * @param correlation  an identifier of the failed activity
   * @param timestamp    time when the error occurred
   * @param includeTrace whether the full stack trace of the exception is included
   * @return initialized error
   */
  public static Error from(final Throwable exception, final Correlation correlation, final long timestamp, final boolean includeTrace) {
    final Throwable root = findRoot(exception);
    final String message = exception.getMessage() == null
        ? exception.getClass().getSimpleName()
        : exception.getMessage();
    final List<ErrorChainElement> errorChain = includeTrace
        ? collectCausalChain(exception)
        : ImmutableList.<ErrorChainElement>of();
    return create(message, Objects.toString(root), correlation, timestamp, errorChain);
  }

  /**
   * Circular reference safe root finder.
   *
   * @param throwable any non-null exception
   * @return root exception or an AssertionError if a circular reference is detected
   */
  private static Throwable findRoot(Throwable throwable) {
    final Set<Throwable> seen = Sets.newIdentityHashSet();
    seen.add(throwable);
    Throwable cause;
    while ((cause = throwable.getCause()) != null) {
      throwable = cause;
      if (!seen.add(throwable)) {
        return new AssertionError(Pretty.format(CIRUCLAR_TEMPLATE, throwable));
      }
    }
    return throwable;
  }

  /**
   * Create an ordered list of the causal chain of errors leading up to the given top level error.
   *
   * @param top top most exception in chain
   * @return full causal chain of exceptions
   */
  private static List<ErrorChainElement> collectCausalChain(final Throwable top) {
    final Set<Throwable> seen = Sets.newIdentityHashSet();
    final ImmutableList.Builder<ErrorChainElement> chain = ImmutableList.builder();
    Throwable current = top;
    while (current != null) {
      if (!seen.add(current)) {
        final AutoValue_Error_ErrorChainElement element = ErrorChainElement.create(
            Pretty.format(CIRUCLAR_TEMPLATE, current), ImmutableList.<StackTraceElement>of());
        chain.add(element);
        break;  // circular reference in chain
      }
      final AutoValue_Error_ErrorChainElement element =
          ErrorChainElement.create(current.toString(), ImmutableList.copyOf(current.getStackTrace()));
      chain.add(element);
      current = current.getCause();
    }
    return chain.build();
  }

  private static final String CIRUCLAR_TEMPLATE = "[CIRCULAR REFERENCE : %s]";

  /**
   * internal use only!
   */
  @JsonCreator
  static Error create(@JsonProperty("message") final String message,
                      @JsonProperty("cause") final String cause,
                      @JsonProperty("logref") final Correlation correlation,
                      @JsonProperty("timestamp") final long timestamp,
                      @JsonProperty("chain") final List<ErrorChainElement> chain) {
    return new AutoValue_Error(message, cause, correlation, timestamp, ImmutableList.copyOf(chain));
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
  @JsonProperty("chain")
  public abstract List<ErrorChainElement> getChain();

  /**
   * Represent on member in a causal chain of exceptions.
   */
  @AutoValue
  public static abstract class ErrorChainElement {
    @JsonCreator
    static AutoValue_Error_ErrorChainElement create(@JsonProperty("exception") final String exception,
                                                    @JsonProperty("trace") final List<StackTraceElement> trace) {
      return new AutoValue_Error_ErrorChainElement(exception, trace);
    }

    ErrorChainElement() { /* prevent sub classes */ }

    /**
     * Description of exception as returned by {@link Throwable#toString()}.
     */
    @JsonProperty("exception")
    public abstract String getException();

    /**
     * Stacktrace of this chain element.
     */
    @JsonProperty("trace")
    public abstract List<StackTraceElement> getStackTrace();
  }
}
