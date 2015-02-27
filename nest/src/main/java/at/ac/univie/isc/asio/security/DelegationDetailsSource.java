package at.ac.univie.isc.asio.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Extract delegated credentials from the {@code 'Delegate-Authorization'} http header. Expects the
 * credentials to be given as defined in the {@code Basic Authentication} scheme.
 * Additionally add an {@link at.ac.univie.isc.asio.security.AuthoritiesFilter} if the request
 * method is {@code GET}.
 */
public final class DelegationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, DelegatedCredentialsDetails> {
  /** the non-standard http header used to transmit delegated credentials */
  static final String DELEGATE_AUTHORIZATION_HEADER = "Delegate-Authorization";
  /** excludes modifying authorities from GET requests */
  static final AuthoritiesFilter GET_RESTRICTION =
      AuthoritiesFilter.exclude(Arrays.<GrantedAuthority>asList(Permission.INVOKE_UPDATE));

  private final BasicAuthIdentityExtractor parser;

  private DelegationDetailsSource(final BasicAuthIdentityExtractor parser) {
    this.parser = parser;
  }

  public static DelegationDetailsSource create() {
    return new DelegationDetailsSource(new BasicAuthIdentityExtractor());
  }

  public DelegatedCredentialsDetails buildDetails(final HttpServletRequest request) {
    final Identity credentials = findDelegatedCredentials(request);
    final DelegatedCredentialsDetails details = new DelegatedCredentialsDetails(credentials);
    applyRestriction(request, details);
    return details;
  }

  private Identity findDelegatedCredentials(final HttpServletRequest request) {
    try {
      final String header = request.getHeader(DELEGATE_AUTHORIZATION_HEADER);
      return parser.authenticate(header);
    } catch (BasicAuthIdentityExtractor.MalformedAuthHeader malformedAuthHeader) {
      throw new BadCredentialsException("illegal 'Delegate-Authorization'", malformedAuthHeader);
    }
  }

  private void applyRestriction(final HttpServletRequest request, final DelegatedCredentialsDetails details) {
    if (HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
      details.setRestriction(GET_RESTRICTION);
    }
  }
}
