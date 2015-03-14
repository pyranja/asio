package at.ac.univie.isc.asio.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;

/**
 * Always yield a fixed authority and no redirection.
 */
public final class FixedAuthorityFinder implements FindAuthorization {
  public static FixedAuthorityFinder create(final GrantedAuthority fixed) {
    return new FixedAuthorityFinder(fixed);
  }

  private final GrantedAuthority fixed;

  private FixedAuthorityFinder(final GrantedAuthority fixed) {
    this.fixed = fixed;
  }

  @Override
  public AuthAndRedirect accept(final HttpServletRequest ignored) throws AuthenticationException {
    return AuthAndRedirect.noRedirect(fixed);
  }
}
