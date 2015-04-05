package at.ac.univie.isc.asio;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Locale;

/**
 * A classification dimension, referring to the type and extent of the classified activity or event.
 */
public enum Scope {
  /** spans the whole asio instance, e.g. configuration changes, startup/shutdown. */
  SYSTEM,
  /** pertains to a single request from a client, e.g. a query execution */
  REQUEST;

  private final Marker marker;

  private Scope() {
    marker = MarkerFactory.getMarker(name());
  }

  /**
   * The {@link org.slf4j.Marker} associated with this scope for logging purposes.
   * @return the marker representing this scope
   */
  public final Marker marker() {
    return marker;
  }

  @Override
  public final String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }
}
