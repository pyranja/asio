package at.ac.univie.isc.asio.tool;

import com.google.common.base.Ticker;

public class TestTicker extends Ticker {
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

  public void setTime(final long time) {
    this.time = time;
  }

  public void advance(final long by) {
    this.time += by;
  }
}
