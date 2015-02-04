package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.IsAuthorized;

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

  public Connector create(final IsAuthorized authorizer, final EventReporter report) {
    return SchedulingConnector.around(schedulerProvider.get(),
        EventfulConnector.around(report,
            ObservableInvoker.adapt(
                ValidatingInvoker.around(authorizer,
                    SelectByLanguage.from(enginesProvider.get())))));
  }

}
