package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.tool.TypedValue;

/**
 * An identifier used to mark correlated events.
 */
public final class Correlation extends TypedValue<String> {
  public static Correlation valueOf(final String val) {
    return new Correlation(val);
  }

  private Correlation(final String val) {
    super(val);
  }
}
