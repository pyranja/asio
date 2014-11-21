package at.ac.univie.isc.asio.tool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

/**
 * A type-safe, time-unit aware container for timeout configuration.
 */
@Immutable
public class TimeoutSpec {
  private static final TimeoutSpec UNDEFINED = new TimeoutSpec(-1);

  /**
   * @return a timeout with no defined value
   */
  @Nonnull
  public static TimeoutSpec undefined() {
    return UNDEFINED;
  }

  /**
   * @param timeout literal value of the timeout
   * @param unit the time unit of the literal value, e.g. {@link java.util.concurrent.TimeUnit#SECONDS}
   * @return timeout with a defined value or {@link #undefined()} if given value is less than zero.
   */
  @Nonnull
  public static TimeoutSpec from(final long timeout, @Nonnull final TimeUnit unit) {
    if (timeout < 0) {
      return UNDEFINED;
    }
    return new TimeoutSpec(unit.toNanos(timeout));
  }

  private final long timeoutInNanos;
  private final boolean defined;

  private TimeoutSpec(final long timeout) {
    this.timeoutInNanos = timeout;
    this.defined = timeout >= 0;
  }

  /**
   * Get the timeout value converted to the given time-unit, or the fallback value, if the timeout
   * is not defined.
   * @param unit target time-unit
   * @param fallback default value
   * @return literal value of timeout in the target unit
   */
  public long getAs(@Nonnull final TimeUnit unit, final long fallback) {
    return defined ? unit.convert(timeoutInNanos, TimeUnit.NANOSECONDS) : fallback;
  }

  /**
   * @return {@code true} if this timeout has a defined value
   */
  public boolean isDefined() {
    return defined;
  }

  @VisibleForTesting
  final long value() {
    return timeoutInNanos;
  }

  /**
   * @return a human readable representation of this timeout.
   */
  @Nonnull
  public final String asText() {
    if (defined) {
      return Long.toString(TimeUnit.NANOSECONDS.toMillis(timeoutInNanos)) + "ms";
    } else {
      return "undefined";
    }
  }

  @Override
  public String toString() {
    final Objects.ToStringHelper builder = Objects.toStringHelper(this);
    if (defined) {
      builder.add("seconds", TimeUnit.NANOSECONDS.toSeconds(timeoutInNanos));
    } else {
      builder.addValue("UNDEFINED");
    }
    return builder.toString();
  }
}
