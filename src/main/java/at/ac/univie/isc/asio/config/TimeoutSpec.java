package at.ac.univie.isc.asio.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;

import java.util.concurrent.TimeUnit;

public class TimeoutSpec {
  public static final TimeoutSpec UNDEFINED = new TimeoutSpec(-1);

  @SuppressWarnings("SameReturnValue")
  public static TimeoutSpec undefined() {
    return UNDEFINED;
  }

  public static TimeoutSpec from(final long timeout, final TimeUnit unit) {
    if (timeout < 0) {
      return UNDEFINED;
    }
    return new TimeoutSpec(unit.toNanos(timeout));
  }

  private final long timeoutInNanos;
  private final boolean defined;

  public TimeoutSpec(final long timeout) {
    this.timeoutInNanos = timeout;
    this.defined = timeout >= 0;
  }

  public long getAs(final TimeUnit unit, final long fallback) {
    return defined ? unit.convert(timeoutInNanos, TimeUnit.NANOSECONDS) : fallback;
  }

  public boolean isDefined() {
    return defined;
  }

  @VisibleForTesting
  final long value() {
    return timeoutInNanos;
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
