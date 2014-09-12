package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.annotation.concurrent.ThreadSafe;
import java.security.Principal;

/**
 * Decorate commands to emit lifecycle events during execution.
 */
@ThreadSafe
public class EventfulCommandDecorator implements Command.Factory {
  private final Command.Factory delegate;
  private final Supplier<EventReporter> scopedEventReporter;

  public EventfulCommandDecorator(final Command.Factory delegate, final Supplier<EventReporter> scopedEventReporter) {
    this.delegate = delegate;
    this.scopedEventReporter = scopedEventReporter;
  }

  @Override
  public Command accept(final Parameters parameters, final Principal owner) {
    final Command original = delegate.accept(parameters, owner);
    return new EventfulCommand(original, scopedEventReporter.get());
  }

  @VisibleForTesting
  static class EventfulCommand implements Command {
    private final Command delegate;
    private final EventReporter report;

    EventfulCommand(final Command delegate, final EventReporter report) {
      this.delegate = delegate;
      this.report = report;
    }

    @Override
    public Role requiredRole() {
      return delegate.requiredRole();
    }

    @Override
    public Multimap<String, String> properties() {
      return delegate.properties();
    }

    @Override
    public Observable<Results> observe() {
      return delegate
          .observe()
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
          .map(new Func1<Results, Results>() {
            @Override
            public Command.Results call(final Command.Results results) {
              return ResultsProxy.wrap(results)
                  .onSuccess(new Action0() {
                    @Override
                    public void call() {
                      report.event(EventReporter.COMPLETED);
                    }
                  })
                  .onError(new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable cause) {
                      report.with(cause).event(EventReporter.FAILED);
                    }
                  });
            }
          });
    }

    @Override
    public final String toString() {
      return delegate.toString();
    }
  }
}
