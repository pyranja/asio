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
   * @param role bundle of permissions assigned to the requester
   * @return wrapped request
   */
  public static AuthorizedRequestProxy wrap(final HttpServletRequest request, final Identity user, final Role role) {
    return new AuthorizedRequestProxy(request, user, role);
  }

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
