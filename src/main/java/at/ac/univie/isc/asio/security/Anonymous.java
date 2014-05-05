package at.ac.univie.isc.asio.security;

import java.security.Principal;

/**
 * Indicate an unavailable {@link Principal}.
 * 
 * @author Chris Borckholder
 */
public final class Anonymous implements Principal {

  public static final Principal INSTANCE = new Anonymous();

  private Anonymous() {}

  @Override
  public String getName() {
    return "anonymous";
  }

  @Override
  public String toString() {
    return "[Anonymous]";
  }
}
