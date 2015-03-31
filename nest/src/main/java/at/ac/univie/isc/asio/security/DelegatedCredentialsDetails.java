package at.ac.univie.isc.asio.security;

import static java.util.Objects.requireNonNull;

/**
 * Hold delegated credentials for authentication against other systems on behalf of the client and
 * an optional {@code AuthoritiesFilter} to apply additional restrictions on the request.
 */
public final class DelegatedCredentialsDetails {
  private final Identity credentials;

  /**
   * Store the given delegated credentials in addition to {@code WebAuthenticationDetails} functions.
   * By default no restrictions are applied.
   *
   * @param delegated delegated credentials
   */
  public DelegatedCredentialsDetails(final Identity delegated) {
    credentials = requireNonNull(delegated, "delegated credentials");
  }

  public Identity getCredentials() {
    return credentials;
  }
}
