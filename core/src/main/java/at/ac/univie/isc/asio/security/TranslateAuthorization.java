package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.tool.Pair;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Translate from an external authorization method to an internal one, by wrapping a
 * {@code HttpServletRequest}/{@code HttpServletResponse} pair.
 */
public interface TranslateAuthorization {
  /**
   * Modify or decorate the given request and response as necessary to adapt the authorization
   * method and inject the given granted role.
   * @param authority authority that should be granted to this request
   * @param request source request
   * @param response source response
   * @return a request/response pair that uses the internal authorization method
   */
  Wrapped translate(GrantedAuthority authority, HttpServletRequest request, HttpServletResponse response);

  /**
   * Hold adapted request and response.
   */
  public static final class Wrapped extends Pair<HttpServletRequest, HttpServletResponse> {
    static Wrapped create(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
      return new Wrapped(httpServletRequest, httpServletResponse);
    }

    private Wrapped(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
      super(httpServletRequest, httpServletResponse);
    }

    public HttpServletRequest request() {
      return first();
    }

    public HttpServletResponse response() {
      return second();
    }
  }
}
