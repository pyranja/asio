package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Optional;
import rx.Observable;
import rx.Subscriber;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;

/**
 * Intercept an {@code Invocation} inside an {@code Observable} and check if the current context is
 * authorized to execute the {@code Invocation}.
 * Block the {@code Invocation} and propagate an error if not authorized.
 */
final class AuthorizingOperator implements Observable.Operator<Invocation, Invocation> {
  private final Optional<SecurityContext> security = SecurityContextHolder.get();

  static AuthorizingOperator create() {
    return new AuthorizingOperator();
  }

  @Override
  public Subscriber<? super Invocation> call(final Subscriber<? super Invocation> child) {
    return new AuthorizingSubscriber(child, this.security);
  }

  private static class AuthorizingSubscriber extends Subscriber<Invocation> {
    private final Subscriber<? super Invocation> child;
    private final Optional<SecurityContext> security;

    public AuthorizingSubscriber(final Subscriber<? super Invocation> child, final Optional<SecurityContext> security) {
      this.child = child;
      this.security = security;
    }

    @Override
    public void onCompleted() {
      child.onCompleted();
    }

    @Override
    public void onError(final Throwable e) {
      child.onError(e);
    }

    @Override
    public void onNext(final Invocation invocation) {
      // TODO : close the invocation on authorization failure ?
      final Role required = invocation.requires();
      if (!security.isPresent()) {
        child.onError(new IllegalStateException("no security context available"));
      } else if (!security.get().isUserInRole(required.name())) {
        child.onError(new ForbiddenException(Pretty.format("requires %s rights", required)));
      } else {  // authorized
        child.onNext(invocation);
      }
    }
  }
}
