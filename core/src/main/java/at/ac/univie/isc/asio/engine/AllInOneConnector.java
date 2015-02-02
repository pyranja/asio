package at.ac.univie.isc.asio.engine;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Execute protocol requests. This implementation wraps all extracted legacy behavior and should
 * behave exactly like the LegacyProtocolResource with EngineRegistry for transition purposes.
 */
@AutoFactory
public class AllInOneConnector implements Connector {
  private final Map<Language, Engine> registry;
  private final Scheduler scheduler;
  private final EventReporter report;
  private final IsAuthorized authorizer;

  public AllInOneConnector(@Provided final Iterable<Engine> engines,
                           @Provided final Scheduler scheduler,
                           final IsAuthorized authorizer,
                           final EventReporter report) {
    this.scheduler = scheduler;
    this.report = report;
    this.authorizer = authorizer;
    this.registry = Maps.uniqueIndex(engines, new Function<Engine, Language>() {
      @Override
      public Language apply(final Engine input) {
        return input.language();
      }
    });
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Parameters parameters) {
    try {
      report.with(parameters).event(EventReporter.RECEIVED);
      final Invocation invocation = invoke(parameters);
      report.with(invocation.properties().asMap()).event(EventReporter.ACCEPTED);
      return observeResults(invocation);
    } catch (final Exception cause) {
      report.with(cause).event(EventReporter.REJECTED);
      return Observable.error(cause);
    }
  }

  private Invocation invoke(final Parameters parameters) {
    parameters.failIfNotValid();
    final Engine delegate = registry.get(parameters.language());
    if (delegate == null) {
      throw new Language.NotSupported(parameters.language());
    }
    final Invocation invocation =
        delegate.prepare(parameters);
    authorizer.check(invocation.requires());
    return invocation;
  }

  private Observable<StreamedResults> observeResults(final Invocation invocation) {
    return Observable.create(OnSubscribeExecute.given(invocation))
        .subscribeOn(scheduler)
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(final Throwable throwable) {
            report.with(throwable).event(EventReporter.FAILED);
          }
        })
        .doOnCompleted(new Action0() {
          @Override
          public void call() {
            report.event(EventReporter.EXECUTED);
          }
        })
        .map(new Func1<StreamedResults, StreamedResults>() {
          @Override
          public StreamedResults call(final StreamedResults results) {
            results.progress()
                .subscribe(Actions.empty(),
                    new Action1<Throwable>() {
                      @Override
                      public void call(final Throwable throwable) {
                        report.with(throwable).event(EventReporter.FAILED);
                      }
                    },
                    new Action0() {
                      @Override
                      public void call() {
                        report.event(EventReporter.COMPLETED);
                      }
                    });
            return results;
          }
        })
        ;
  }
}
