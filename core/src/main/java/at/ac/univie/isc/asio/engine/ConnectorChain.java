package at.ac.univie.isc.asio.engine;

import javax.inject.Inject;
import javax.inject.Provider;

public final class ConnectorChain {
  private final Provider<Iterable<Engine>> enginesProvider;
  private final Provider<rx.Scheduler> schedulerProvider;

  @Inject
  public ConnectorChain(final Provider<Iterable<Engine>> enginesProvider, final Provider<rx.Scheduler> schedulerProvider) {
    this.enginesProvider = enginesProvider;
    this.schedulerProvider = schedulerProvider;
  }

  public Connector create(final EventReporter report) {
    return EventfulConnector.around(report,
        new ReactiveConnector(SelectByLanguage.from(enginesProvider.get()), schedulerProvider.get()));
  }
}
