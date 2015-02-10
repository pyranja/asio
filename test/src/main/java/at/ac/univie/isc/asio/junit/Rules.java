package at.ac.univie.isc.asio.junit;

import at.ac.univie.isc.asio.web.HttpServer;
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

  /**
   * A light-weight, in-process http server.
   *
   * @param label describing this server
   * @return A new {@link at.ac.univie.isc.asio.web.HttpServer} rule
   */
  public static HttpServer httpServer(final String label) {
    return HttpServer.create(label);
  }

  /**
   * Enrich failure descriptions with reports from test collaborators.
   *
   * @return A new empty {@link Interactions} rule
   */
  public static Interactions interactions() {
    return Interactions.empty();
  }
}
