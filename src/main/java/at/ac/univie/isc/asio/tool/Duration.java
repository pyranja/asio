package at.ac.univie.isc.asio.tool;

import java.util.concurrent.TimeUnit;

/**
 * A temporal duration, measured as a long value in a specific {@link java.util.concurrent.TimeUnit}.
 */
public final class Duration extends Pair<Long, TimeUnit> {
  public static Duration create(final Long length, final TimeUnit unit) {
    return new Duration(length, unit);
  }

  private Duration(final Long length, final TimeUnit unit) {
    super(length, unit);
  }

  /**
   * @return length of this duration
   */
  public long length() {
    return first();
  }

  /**
   * @return unit of this duration
   */
  public TimeUnit unit() {
    return second();
  }
}
