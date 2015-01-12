package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.security.Role;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

/**
 * Determine whether a client is authorized to invoke the requested command.
 */
final class CheckCommandAuthorization {
  private final SecurityContext security;
  private final Request request;

  private CheckCommandAuthorization(final SecurityContext security, final Request request) {
    this.security = security;
    this.request = request;
  }

  public static CheckCommandAuthorization with(final SecurityContext security, final Request request) {
    return new CheckCommandAuthorization(security, request);
  }

  /**
   * Determine whether the given command may be executed.
   *
   * @param command requested
   * @return true if authorized
   * @throws javax.ws.rs.ForbiddenException if client is not authorized
   */
  public boolean check(final Command command) {
    final Role required = command.requiredRole();
    ensure(security.isUserInRole(required.name()));
    if (HttpMethod.GET.equalsIgnoreCase(request.getMethod())) {
      ensure(Permission.READ.grants(required));
    }
    return true;
  }

  private void ensure(final boolean condition) {
    if (!condition) { throw new ForbiddenException(); }
  }
}
