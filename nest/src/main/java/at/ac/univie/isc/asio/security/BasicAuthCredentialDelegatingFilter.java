package at.ac.univie.isc.asio.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Extract principal and credentials from the basic auth in the {@code Authorization} header.
 */
public final class BasicAuthCredentialDelegatingFilter extends AbstractPreAuthenticatedProcessingFilter {
  private final BasicAuthIdentityExtractor parser;

  BasicAuthCredentialDelegatingFilter(final BasicAuthIdentityExtractor parser) {
    this.parser = parser;
  }

  public static BasicAuthCredentialDelegatingFilter create(final AuthenticationManager authenticationManager) {
    final BasicAuthCredentialDelegatingFilter filter = new BasicAuthCredentialDelegatingFilter(new BasicAuthIdentityExtractor());
    filter.setAuthenticationManager(authenticationManager);
    return filter;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    final Identity credentials = extractFrom(request);
    return credentials.isDefined() ? credentials.getName() : null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
    return extractFrom(request);
  }

  private Identity extractFrom(final HttpServletRequest request) {
    try {
      final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      return parser.authenticate(header);
    } catch (BasicAuthIdentityExtractor.MalformedAuthHeader malformedAuthHeader) {
      throw new BadCredentialsException("illegal pre authentication", malformedAuthHeader);
    }
  }
}
