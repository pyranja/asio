package at.ac.univie.isc.asio.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Override a {@link javax.servlet.http.HttpServletRequest}'s auth methods and delegate to asio
 * security primitives.
 */
final class AuthorizedRequestProxy extends HttpServletRequestWrapper {
  /**
   * Override auth methods in given request with given Token and Permission.
   * @param request to be wrapped
   * @param user represent the requester
   * @param permission bundle of roles assigned to the requester
   * @return wrapped request
   */
  public static AuthorizedRequestProxy wrap(final HttpServletRequest request, final Token user, final Permission permission) {
    return new AuthorizedRequestProxy(request, user, permission);
  }

  private final Token user;
  private final Permission permission;

  private AuthorizedRequestProxy(final HttpServletRequest request, final Token user, final Permission permission) {
    super(request);
    this.user = user;
    this.permission = permission;
  }

  @Override
  public Token getUserPrincipal() {
    return user;
  }

  @Override
  public String getRemoteUser() {
    return user.getName();
  }

  @Override
  public String getAuthType() {
    return HttpServletRequest.BASIC_AUTH;
  }

  @Override
  public boolean isUserInRole(final String role) {
    assert role != null : "got null role";
    return permission.grants(Role.valueOf(role));
  }
}
