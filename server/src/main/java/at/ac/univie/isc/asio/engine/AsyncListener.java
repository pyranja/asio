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

import rx.Subscription;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;

/**
 * Unsubscribe from a given {@link rx.Subscription} on any terminal event on an
 * {@link javax.ws.rs.container.AsyncResponse}.
 */
final class AsyncListener implements TimeoutHandler, CompletionCallback, ConnectionCallback {
  private final Subscription subscription;

  private AsyncListener(final Subscription subscription) {
    this.subscription = subscription;
  }

  /**
   * Create an {@code AsyncListener}, which will unsubscribe from the given {@code Subscription}.
   * @param subscription will be unsubscribed from
   * @return listener instance
   */
  public static AsyncListener cleanUp(final Subscription subscription) {
    return new AsyncListener(subscription);
  }

  /**
   * Attach this listener to the given {@code AsyncResponse}.
   * @param async target response
   * @return attached listener
   */
  public AsyncListener listenTo(final AsyncResponse async) {
    async.setTimeoutHandler(this);
    async.register(this);
    return this;
  }

  @Override
  public void handleTimeout(final AsyncResponse asyncResponse) {
    asyncResponse.resume(new ServiceUnavailableException("execution time limit exceeded"));
    subscription.unsubscribe();
  }

  @Override
  public void onComplete(final Throwable throwable) {
    subscription.unsubscribe();
  }

  @Override
  public void onDisconnect(final AsyncResponse disconnected) {
    subscription.unsubscribe();
  }
}
