/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
