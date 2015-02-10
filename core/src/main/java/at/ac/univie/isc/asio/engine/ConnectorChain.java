package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.insight.Emitter;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Provider;

public final class ConnectorChain {
  private final Provider<Iterable<Engine>> enginesProvider;
  private final Provider<rx.Scheduler> schedulerProvider;
  private final Provider<Emitter> eventingProvider;

  @Inject
  public ConnectorChain(final Provider<Iterable<Engine>> enginesProvider, final Provider<Scheduler> schedulerProvider, final Provider<Emitter> eventingProvider) {
    this.enginesProvider = enginesProvider;
    this.schedulerProvider = schedulerProvider;
    this.eventingProvider = eventingProvider;
  }

  public Connector create() {
    return EventfulConnector.around(eventingProvider.get(),
        ReactiveInvoker.from(
            FixedSelection.from(enginesProvider.get()),
            schedulerProvider.get(),
            JaxrsAuthorizer.create()
        )
    );
  }
}
