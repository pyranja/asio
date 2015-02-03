package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Predicate;

import javax.ws.rs.ForbiddenException;

/**
 * Devorate an invoker and perform generic validation of input {@code Parameters} and the resulting
 * {@code Invocation}.
 */
public final class ValidatingInvoker implements Invoker {
  private final Invoker delegate;
  private final Predicate<Role> authorized;

  private ValidatingInvoker(final Invoker delegate, final Predicate<Role> authorized) {
    this.delegate = delegate;
    this.authorized = authorized;
  }

  public static ValidatingInvoker around(final Predicate<Role> authorized, final Invoker delegate) {
    return new ValidatingInvoker(delegate, authorized);
  }

  @Override
  public Invocation prepare(final Parameters parameters) {
    parameters.failIfNotValid();
    final Invocation invocation = delegate.prepare(parameters);
    ensureAuthorized(invocation.requires());
    return invocation;
  }

  private void ensureAuthorized(final Role required) {
    if (!authorized.apply(required)) {
      throw new ForbiddenException(Pretty.format("requires %s rights", required));
    }
  }
}
