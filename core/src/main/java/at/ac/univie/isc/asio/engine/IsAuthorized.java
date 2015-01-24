package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Predicate;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

/**
 * Determine whether a client is authorized to invoke operation which may require special permissions.
 */
public final class IsAuthorized implements Predicate<Role> {
  private final SecurityContext security;
  private final Request request;

  private IsAuthorized(final SecurityContext security, final Request request) {
    this.security = security;
    this.request = request;
  }

  public static IsAuthorized given(final SecurityContext security, final Request request) {
    return new IsAuthorized(security, request);
  }

  /**
   * Determine whether the given {@code Role} requirement is satisfied by the current context.
   *
   * @param required role required for an action
   * @return true if the given role is satisfied by the context
   */
  @Override
  public boolean apply(final Role required) {
    return security.isUserInRole(required.name()) && (!isReadOnlyRequest()
        || Permission.READ.grants(required));
  }

  private boolean isReadOnlyRequest() {
    return HttpMethod.GET.equalsIgnoreCase(request.getMethod());
  }

  /**
   * Determine whether the given {@code Role} requirement is satisfied by the current context. This
   * will fail fast if the client is not authorized for the given role.
   *
   * @param required role required for an action
   * @return true if the given role is satisfied by the context
   * @throws javax.ws.rs.ForbiddenException if not authorized
   */
  public boolean check(final Role required) {
    if (!apply(required)) { throw new ForbiddenException(Pretty.format("requires %s rights", required)); }
    return true;
  }
}
