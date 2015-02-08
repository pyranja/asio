package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Optional;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;

/**
 * Devorate an invoker and perform generic validation of input {@code Parameters} and the resulting
 * {@code Invocation}.
 */
public final class ValidatingInvoker implements Invoker {
  private final Invoker delegate;

  private ValidatingInvoker(final Invoker delegate) {
    this.delegate = delegate;
  }

  public static ValidatingInvoker around(final Invoker delegate) {
    return new ValidatingInvoker(delegate);
  }

  @Override
  public Invocation prepare(final Parameters parameters) {
    parameters.failIfNotValid();
    final Invocation invocation = delegate.prepare(parameters);
    ensureAuthorized(invocation.requires());
    return invocation;
  }

  private void ensureAuthorized(final Role required) {
    final Optional<SecurityContext> security = SecurityContextHolder.get();
    if (!security.isPresent()) {
      throw new IllegalStateException("no security context available");
    } else if (!security.get().isUserInRole(required.name())) {
      throw new ForbiddenException(Pretty.format("requires %s rights", required));
    }
    // authorized
  }
}
