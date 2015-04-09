package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.insight.Operation;
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
  private final Emitter event;

  private EventfulConnector(final Connector delegate, final Emitter event) {
    this.delegate = delegate;
    this.event = event;
  }

  /**
   * Wrap a given {@code Connector} by emitting request events to the set event sink.
   * @param emitter event sink
   * @param delegate wrapped original connector
   * @return decorated connector
   */
  public static EventfulConnector around(final Emitter emitter, final Connector delegate) {
    return new EventfulConnector(delegate, emitter);
  }

  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Command command) {
    event.emit(Operation.received(command));
    return delegate.accept(command)
        .doOnError(EmitError.to(event))
        .doOnNext(EmitExecuted.to(event))
        .map(ConvertToEventfulResults.with(event));
  }

  static class ConvertToEventfulResults implements Func1<StreamedResults, StreamedResults> {
    private final Emitter event;

    private ConvertToEventfulResults(final Emitter event) {
      this.event = event;
    }

    static ConvertToEventfulResults with(final Emitter event) {
      return new ConvertToEventfulResults(event);
    }

    @Override
    public StreamedResults call(final StreamedResults results) {
      results.progress()
          .subscribe(Actions.empty(),
              new Action1<Throwable>() {
                @Override
                public void call(final Throwable throwable) {
                  event.emit(Operation.failure(throwable));
                }
              },
              new Action0() {
                @Override
                public void call() {
                  event.emit(Operation.completed());
                }
              });
      return results;
    }
  }


  static class EmitExecuted implements Action1<StreamedResults> {
    private final Emitter event;

    private EmitExecuted(final Emitter event) {
      this.event = event;
    }

    static EmitExecuted to(final Emitter event) {
      return new EmitExecuted(event);
    }

    @Override
    public void call(final StreamedResults ignored) {
      event.emit(Operation.executed());
    }
  }

  static class EmitError implements Action1<Throwable> {
    private final Emitter event;

    private EmitError(final Emitter event) {
      this.event = event;
    }

    static EmitError to(final Emitter event) {
      return new EmitError(event);
    }

    @Override
    public void call(final Throwable throwable) {
      event.emit(Operation.failure(throwable));
    }
  }
}
