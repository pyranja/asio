package at.ac.univie.isc.asio.security;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Delegates to a given {@code SecurityContext}, but takes the request method into account,
 * when deciding if a user is authorized.
 */
// FIXME replace with non-delegating version later
public final class IncludeRequestMethodSecurityContext implements SecurityContext {
  private final SecurityContext delegate;
  private final Request request;

  private IncludeRequestMethodSecurityContext(final SecurityContext delegate, final Request request) {
    this.delegate = delegate;
    this.request = request;
  }

  public static IncludeRequestMethodSecurityContext wrap(final SecurityContext delegate, final Request request) {
    return new IncludeRequestMethodSecurityContext(delegate, request);
  }

  @Override
  public Principal getUserPrincipal() {
    return delegate.getUserPrincipal();
  }

  @Override
  public boolean isUserInRole(final String role) {
    return IsAuthorized.given(delegate, request).apply(Role.valueOf(role));
  }

  @Override
  public boolean isSecure() {
    return delegate.isSecure();
  }

  @Override
  public String getAuthenticationScheme() {
    return delegate.getAuthenticationScheme();
  }
}
