package at.ac.univie.isc.asio.junit;

import com.google.common.primitives.Ints;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

public final class Rules {
  private Rules() {}

  /**
   * @param value max duration of test
   * @param unit unit of the max duration
   * @return A new {@link org.junit.rules.Timeout} rule with the given value.
   */
  public static Timeout timeout(final int value, final TimeUnit unit) {
    return new Timeout(Ints.checkedCast(unit.toMillis(value)));
  }
}
