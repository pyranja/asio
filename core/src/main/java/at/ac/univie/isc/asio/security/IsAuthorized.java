package at.ac.univie.isc.asio.security;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import java.util.Locale;

/**
 * Determine whether a client is authorized to invoke operation which may require special permissions.
 */
public final class IsAuthorized implements Predicate<Permission> {
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
  public boolean apply(final Permission required) {
    return satisfies(required) && (methodAllowsModifications() || onlyReadPermission(required));
  }

  private boolean satisfies(final Permission required) {
    return security.isUserInRole(required.name());
  }

  private static final ImmutableSet<String> MODIFYING_METHODS =
      ImmutableSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);

  private boolean methodAllowsModifications() {
    return MODIFYING_METHODS.contains(request.getMethod().toUpperCase(Locale.ENGLISH));
  }

  private boolean onlyReadPermission(final Permission required) {
    return Role.READ.grants(required);
  }
}
