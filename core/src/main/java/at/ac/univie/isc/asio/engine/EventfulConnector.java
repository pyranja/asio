package at.ac.univie.isc.asio.engine;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

import javax.annotation.Nonnull;

/**
 * Decorate a {@code Connector} by attaching event emitting listeners to the created
 * {@code Observable}.
 */
public final class EventfulConnector implements Connector {
  private final Connector delegate;
  private final EventReporter report;

  private EventfulConnector(final Connector delegate, final EventReporter report) {
    this.delegate = delegate;
    this.report = report;
  }

  /**
   * Wrap a given {@code Connector} by emitting request events to the set event sink.
   * @param report event sink
   * @param delegate wrapped original connector
   * @return decorated connector
   */
  public static EventfulConnector around(final EventReporter report, final Connector delegate) {
    return new EventfulConnector(delegate, report);
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Parameters parameters) {
    report.with(parameters).event("received");
    return delegate.accept(parameters)
        .doOnError(EmitError.to(report))
        .doOnNext(EmitExecuted.to(report))
        .map(ConvertToEventfulResults.with(report));
  }

  private static class ConvertToEventfulResults implements Func1<StreamedResults, StreamedResults> {
    private final EventReporter report;

    private ConvertToEventfulResults(final EventReporter report) {
      this.report = report;
    }

    private static ConvertToEventfulResults with(final EventReporter report) {
      return new ConvertToEventfulResults(report);
    }

    @Override
    public StreamedResults call(final StreamedResults results) {
      results.progress()
          .subscribe(Actions.empty(),
              new Action1<Throwable>() {
                @Override
                public void call(final Throwable throwable) {
                  report.with(throwable).event("failed");
                }
              },
              new Action0() {
                @Override
                public void call() {
                  report.event("completed");
                }
              });
      return results;
    }
  }


  private static class EmitExecuted implements Action1<StreamedResults> {
    private final EventReporter report;

    private EmitExecuted(final EventReporter report) {
      this.report = report;
    }

    private static EmitExecuted to(final EventReporter report) {
      return new EmitExecuted(report);
    }

    @Override
    public void call(final StreamedResults ignored) {
      report.event("executed");
    }
  }

  private static class EmitError implements Action1<Throwable> {
    private final EventReporter report;

    private EmitError(final EventReporter report) {
      this.report = report;
    }

    private static EmitError to(final EventReporter report) {
      return new EmitError(report);
    }

    @Override
    public void call(final Throwable throwable) {
      report.with(throwable).event("failed");
    }
  }
}
