package at.ac.univie.isc.asio.platform;

import com.google.common.base.Ticker;

/**
 * {@code Ticker} that reads the system wall time with up to millisecond precision.
 *
 * @see System#currentTimeMillis()
 */
public final class CurrentTime extends Ticker {
  private static final CurrentTime INSTANCE = new CurrentTime();

  public static CurrentTime instance() {
    return INSTANCE;
  }

  private CurrentTime() {}

  @Override
  public long read() {
    return System.currentTimeMillis();
  }
}
