package at.ac.univie.isc.asio.tool;

import com.google.common.base.Ticker;

/**
 * {@code Ticker} that reads the system wall time with up to millisecond precision.
 *
 * @see System#currentTimeMillis()
 */
public final class SystemTime extends Ticker {
  private static final SystemTime INSTANCE = new SystemTime();

  public static SystemTime instance() {
    return INSTANCE;
  }

  private SystemTime() {}

  @Override
  public long read() {
    return System.currentTimeMillis();
  }
}
