package at.ac.univie.isc.asio.tool;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

/**
 * A temporal duration, measured as a long value in a specific {@link java.util.concurrent.TimeUnit}.
 */
@Immutable
public final class Duration extends Pair<Long, TimeUnit> {
  public static Duration create(@Nonnull final Long length, @Nonnull TimeUnit unit) {
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
  @Nonnull
  public TimeUnit unit() {
    return second();
  }
}
