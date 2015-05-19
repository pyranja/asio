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

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.spring.ContextPropagator;
import org.slf4j.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.annotation.Nonnull;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Turn an invocation into an observable execution on set {@link rx.Scheduler}. The execution engine
 * is selected from the set {@link EngineRouter}. The set
 * {@link at.ac.univie.isc.asio.security.Authorizer} checks if the client is permitted to execute
 * the requested operation.
 */
public final class ReactiveInvoker implements Connector {
  private static final Logger log = getLogger(ReactiveInvoker.class);

  private final EngineRouter router;
  private final Authorizer authorizer;
  private final Scheduler scheduler;

  private ReactiveInvoker(final EngineRouter router, final Scheduler scheduler, final Authorizer authorizer) {
    this.router = router;
    this.authorizer = authorizer;
    this.scheduler = scheduler;
  }

  public static ReactiveInvoker from(final EngineRouter router, final Scheduler scheduler, final Authorizer authorizer) {
    return new ReactiveInvoker(router, scheduler, authorizer);
  }

  /**
   * Invoke the requested operation on an appropriate {@link at.ac.univie.isc.asio.engine.Engine},
   * check authorization and provide {@link rx.Observable observable} results.
   *
   * @param command requested operation
   * @return observale results
   */
  @Nonnull
  @Override
  public Observable<StreamedResults> accept(@Nonnull final Command command) {
    try {
      log.debug(Scope.REQUEST.marker(), "received command {}", command);
      command.failIfNotValid();
      final Invocation invocation = router.select(command).prepare(command);
      log.debug(Scope.REQUEST.marker(), "prepared invocation {}", invocation);
      authorizer.check(invocation);
      return Observable.create(runInCurrentContext(OnSubscribeExecute.given(invocation))).subscribeOn(scheduler);
    } catch (final Throwable cause) {
      log.debug(Scope.REQUEST.marker(), "invoking failed {}", cause);
      return Observable.error(cause);
    }
  }

  private <TYPE> Observable.OnSubscribe<TYPE> runInCurrentContext(final Observable.OnSubscribe<TYPE> delegate) {
    final ContextPropagator context = ContextPropagator.capture();
    return new Observable.OnSubscribe<TYPE>() {
      @Override
      public void call(final Subscriber<? super TYPE> subscriber) {
        try (final ContextPropagator ignored = context.publish()) {
          delegate.call(subscriber);
        }
      }
    };
  }
}
