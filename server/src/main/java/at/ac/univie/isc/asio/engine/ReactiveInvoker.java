package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.spring.ContextPropagator;
import org.slf4j.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.annotation.Nonnull;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Turn an invocation into an observable execution on set {@link rx.Scheduler}. The execution engine
 * is selected from the set {@link EngineRouter}. The set
 * {@link at.ac.univie.isc.asio.security.Authorizer} checks if the client is permitted to execute
 * the requested operation.
 */
public final class ReactiveInvoker implements Connector {
  private static final Logger log = getLogger(ReactiveInvoker.class);

  private final EngineRouter router;
  private final Authorizer authorizer;
  private final Scheduler scheduler;

  private ReactiveInvoker(final EngineRouter router, final Scheduler scheduler, final Authorizer authorizer) {
    this.router = router;
    this.authorizer = authorizer;
    this.scheduler = scheduler;
  }

  public static ReactiveInvoker from(final EngineRouter router, final Scheduler scheduler, final Authorizer authorizer) {
    return new ReactiveInvoker(router, scheduler, authorizer);
  }

  /**
   * Invoke the requested operation on an appropriate {@link at.ac.univie.isc.asio.engine.Engine},
   * check authorization and provide {@link rx.Observable observable} results.
   *
   * @param command requested operation
   * @return observale results
   */
  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Command command) {
    try {
      log.debug(Scope.REQUEST.marker(), "received command {}", command);
      command.failIfNotValid();
      final Invocation invocation = router.select(command).prepare(command);
      log.debug(Scope.REQUEST.marker(), "prepared invocation {}", invocation);
      authorizer.check(invocation);
      return Observable.create(runInCurrentContext(OnSubscribeExecute.given(invocation))).subscribeOn(scheduler);
    } catch (final Throwable cause) {
      log.debug(Scope.REQUEST.marker(), "invoking failed {}", cause);
      return Observable.error(cause);
    }
  }

  private <TYPE> Observable.OnSubscribe<TYPE> runInCurrentContext(final Observable.OnSubscribe<TYPE> delegate) {
    final ContextPropagator context = ContextPropagator.capture();
    return new Observable.OnSubscribe<TYPE>() {
      @Override
      public void call(final Subscriber<? super TYPE> subscriber) {
        try (final ContextPropagator ignored = context.publish()) {
          delegate.call(subscriber);
        }
      }
    };
  }
}
