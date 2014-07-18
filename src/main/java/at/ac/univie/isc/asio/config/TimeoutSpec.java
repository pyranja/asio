package at.ac.univie.isc.asio.config;

import com.google.common.base.Objects;

import java.util.concurrent.TimeUnit;

/**
 * @author cb
 */
public class TimeoutSpec {

  public static final TimeoutSpec UNDEFINED = new TimeoutSpec(-1) {
    @Override
    public long getAs(final TimeUnit unit) {
      return -1L;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue("UNDEFINED").toString();
    }
  };

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

  public TimeoutSpec(final long timeout) {
    this.timeoutInNanos = timeout;
  }

  public long getAs(TimeUnit unit) {
    return unit.convert(timeoutInNanos, TimeUnit.NANOSECONDS);
  }

  final long value() {
    return timeoutInNanos;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("seconds", TimeUnit.NANOSECONDS.toSeconds(timeoutInNanos))
        .toString();
  }
}
