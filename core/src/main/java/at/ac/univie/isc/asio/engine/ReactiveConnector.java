package at.ac.univie.isc.asio.engine;

import rx.Observable;
import rx.Scheduler;

import javax.annotation.Nonnull;

public final class ReactiveConnector implements Connector {
  private final Invoker delegate;
  private final Scheduler scheduler;

  public ReactiveConnector(final Invoker delegate, final Scheduler scheduler) {
    this.delegate = delegate;
    this.scheduler = scheduler;
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Parameters parameters) {
    try {
      parameters.failIfNotValid();
      final Invocation invocation = delegate.prepare(parameters);
      return Observable.just(invocation)
          .lift(AuthorizingOperator.create())
          .flatMap(OnSubscribeExecute.fromInvocation())
          .subscribeOn(scheduler);
    } catch (final Exception e) {
      return Observable.error(e);
    }
  }
}
