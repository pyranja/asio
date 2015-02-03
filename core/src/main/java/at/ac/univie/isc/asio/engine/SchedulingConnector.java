package at.ac.univie.isc.asio.engine;

import rx.Observable;
import rx.Scheduler;

import javax.annotation.Nonnull;

/**
 * Decorate a {@code Connector} by using a set {@code Scheduler} for subscribing.
 */
public final class SchedulingConnector implements Connector {
  private final Connector delegate;
  private final Scheduler scheduler;

  private SchedulingConnector(final Connector delegate, final Scheduler scheduler) {
    this.delegate = delegate;
    this.scheduler = scheduler;
  }

  /**
   * Wrap given {@code Connector} by scheduling subscriptions to happen on the set scheduler.
   * @param scheduler scheduler used for subscribing
   * @param delegate wrapped original connector
   * @return decorated connector
   */
  public static SchedulingConnector around(final Scheduler scheduler, final Connector delegate) {
    return new SchedulingConnector(delegate, scheduler);
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Parameters parameters) {
    return delegate.accept(parameters).subscribeOn(scheduler);
  }
}
