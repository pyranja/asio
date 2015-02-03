package at.ac.univie.isc.asio.engine;

import rx.Observable;

import javax.annotation.Nonnull;

/**
 * Adapt an {@code Invoker} to turn {@code Invocations} into observable sequences of results.
 */
public class ObservableInvoker implements Connector {
  private final Invoker invoker;

  private ObservableInvoker(final Invoker invoker) {
    this.invoker = invoker;
  }

  public static ObservableInvoker adapt(final Invoker invoker) {
    return new ObservableInvoker(invoker);
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Parameters parameters) {
    try {
      final Invocation invocation = invoker.prepare(parameters);
      return Observable.create(OnSubscribeExecute.given(invocation));
    } catch (final Exception cause) {
      return Observable.error(cause);
    }
  }
}
