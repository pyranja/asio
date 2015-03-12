package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.tool.Pair;
import com.google.common.base.Optional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletRequest;

/**
 * Extract non-standard authorization information from a http request.
 */
public interface FindAuthorization {

  /**
   * Extract authorization from the given request and determine if a redirection is required.
   *
   * @param request http request
   * @return extracted authorization and a redirection target if needed
   * @throws AuthenticationException if the required authorization information is not found in the
   *                                 request
   */
  AuthAndRedirect accept(HttpServletRequest request) throws AuthenticationException;

  /**
   * Hold extracted authority and redirection target.
   */
  public static final class AuthAndRedirect extends Pair<GrantedAuthority, Optional<String>> {
    /**
     * Use name of {@link Role#NONE} as null authority
     */
    public static SimpleGrantedAuthority NO_AUTHORITY = new SimpleGrantedAuthority(Role.NONE.name());

    private AuthAndRedirect(final GrantedAuthority authority, final Optional<String> redirection) {
      super(authority, redirection);
    }

    public static AuthAndRedirect create(final GrantedAuthority authority, final String tail) {
      return new AuthAndRedirect(authority, Optional.fromNullable(tail));
    }

    public static AuthAndRedirect noRedirect(final GrantedAuthority authority) {
      return new AuthAndRedirect(authority, Optional.<String>absent());
    }

    /**
     * The authority that was embedded in the parsed URI.
     */
    public final GrantedAuthority authority() {
      return first();
    }

    /**
     * The relative URI designating the actual target of the request, if it has to be redirected.
     */
    public final Optional<String> redirection() {
      return second();
    }
  }
}
