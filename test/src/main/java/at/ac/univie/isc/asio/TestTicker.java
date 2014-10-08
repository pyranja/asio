package at.ac.univie.isc.asio;

import com.google.common.base.Ticker;

/**
 * A fake {@code Ticker} for testing, where the time value can be set and modified. The value will
 * not change by itself.
 */
public class TestTicker extends Ticker {
  /**
   * @param initial time value
   * @return fake ticker starting with the given time value
   */
  public static TestTicker create(final long initial) {
    return new TestTicker(initial);
  }

  private long time;

  private TestTicker(final long initial) {
    this.time = initial;
  }

  @Override
  public long read() {
    return time;
  }

  /**
   * Set the time to a fixed value.
   * @param time new time value
   */
  public void setTime(final long time) {
    this.time = time;
  }

  /**
   * Increase the time value by the given amount.
   * @param by increase by this value
   */
  public void advance(final long by) {
    this.time += by;
  }
}
