package at.ac.univie.isc.asio.security;

import org.springframework.security.core.authority.AuthorityUtils;

import java.security.Principal;

import static java.util.Objects.requireNonNull;

/**
 * Hold delegated credentials for authentication against other systems on behalf of the client and
 * an optional {@code AuthoritiesFilter} to apply additional restrictions on the request.
 */
public final class DelegatedCredentialsDetails {
  /** will not remove any authorities */
  static final AuthoritiesFilter NO_RESTRICTION = AuthoritiesFilter.exclude(AuthorityUtils.NO_AUTHORITIES);

  private final Principal credentials;
  private AuthoritiesFilter restriction = NO_RESTRICTION;

  /**
   * Store the given delegated credentials in addition to {@code WebAuthenticationDetails} functions.
   * By default no restrictions are applied.
   *
   * @param delegated delegated credentials
   */
  public DelegatedCredentialsDetails(final Principal delegated) {
    credentials = requireNonNull(delegated, "delegated credentials");
  }

  public Principal getCredentials() {
    return credentials;
  }

  public AuthoritiesFilter getRestriction() {
    return restriction;
  }

  void setRestriction(final AuthoritiesFilter restriction) {
    this.restriction = requireNonNull(restriction);
  }
}
