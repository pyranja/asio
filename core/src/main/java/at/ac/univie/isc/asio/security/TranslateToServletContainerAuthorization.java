package at.ac.univie.isc.asio.security;

import com.google.common.base.Converter;
import com.google.common.base.Objects;
import org.apache.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Use the requests' basic auth credentials and the given role to override the standard servlet
 * authorization methods.
 */
public class TranslateToServletContainerAuthorization implements TranslateAuthorization {
  private final Converter<String, Identity> basicAuth;

  public static TranslateToServletContainerAuthorization newInstance() {
    return new TranslateToServletContainerAuthorization(BasicAuthConverter.fromString());
  }

  private TranslateToServletContainerAuthorization(final Converter<String, Identity> basicAuth) {
    this.basicAuth = basicAuth;
  }

  @Override
  public Wrapped translate(final GrantedAuthority authority, final HttpServletRequest request, final HttpServletResponse response) {
    final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    final Identity principal = Objects.firstNonNull(basicAuth.convert(header), Identity.undefined());
    final Role role = Role.fromAuthority(authority);  // translate to asio conventions
    final AuthorizedRequestProxy requestProxy = new AuthorizedRequestProxy(request, principal, role);
    return Wrapped.create(requestProxy, response);
  }

  /**
   * Override a {@link javax.servlet.http.HttpServletRequest}'s auth methods and delegate to asio
   * security primitives.
   */
  private static final class AuthorizedRequestProxy extends HttpServletRequestWrapper {
    private final Identity user;
    private final Role role;

    private AuthorizedRequestProxy(final HttpServletRequest request, final Identity user, final Role role) {
      super(request);
      this.user = user;
      this.role = role;
    }

    @Override
    public Identity getUserPrincipal() {
      return user;
    }

    @Override
    public String getRemoteUser() {
      return user.nameOrIfUndefined("anonymous");
    }

    @Override
    public String getAuthType() {
      return HttpServletRequest.BASIC_AUTH;
    }

    @Override
    public boolean isUserInRole(final String role) {
      assert role != null : "got null role";
      return this.role.grants(Permission.valueOf(role));
    }
  }
}
