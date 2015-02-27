package at.ac.univie.isc.asio.security;

import org.slf4j.Logger;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Extend the {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider} to
 * detect delegated credentials.
 * <p>
 *   When delegated credential are detected, the regular
 *   {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken} is
 *   replaced by a {@link org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken},
 *   holding the delegated credentials.
 *   Therefore a client is authenticated using a set of credentials that may be unique to this
 *   service and during request processing the service can act on behalf of the client with another
 *   set of credentials.
 * </p>
 */
public final class DelegationAwareAuthenticationProvider extends DaoAuthenticationProvider {
  private static final Logger log = getLogger(DelegationAwareAuthenticationProvider.class);

  /**
   * If the authentication holds {@link at.ac.univie.isc.asio.security.DelegatedCredentialsDetails},
   * inject the delegated credentials into the authentication token.
   * Also apply any {@link at.ac.univie.isc.asio.security.AuthoritiesFilter restriction} found in
   * the details to the mapped user authorities.
   *
   * @param principal the identity of the authenticated client
   * @param authentication that was presented to the provider for validation
   * @param user that was loaded by the implementation
   *
   * @return the successful authentication token
   */
  @Override
  protected Authentication createSuccessAuthentication(final Object principal, final Authentication authentication, final UserDetails user) {
    Authentication result = super.createSuccessAuthentication(principal, authentication, user);
    // must use auth token from parent to get mapped user authorities (authoritiesMapper field is private)
    if (authentication.getDetails() instanceof DelegatedCredentialsDetails) {
      final DelegatedCredentialsDetails details = (DelegatedCredentialsDetails) authentication.getDetails();
      final Set<GrantedAuthority> filtered = details.getRestriction().mapAuthorities(result.getAuthorities());
      result = new PreAuthenticatedAuthenticationToken(principal, details.getCredentials(), filtered);
      log.debug("injecting delegated credentials {}", result);
    } else {
      log.debug("no delegated credentials found in {}", authentication);
    }
    return result;
  }
}
