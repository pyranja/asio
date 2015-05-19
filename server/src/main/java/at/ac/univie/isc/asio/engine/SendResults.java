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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * Send observed {@link at.ac.univie.isc.asio.engine.StreamedResults} as asynchronous response.
 */
final class SendResults extends Subscriber<StreamedResults> {
  private static final Logger log = LoggerFactory.getLogger(SendResults.class);

  /**
   * Create an instance sending result to the given {@code AsyncResponse}.
   * @param async response continuation
   * @return created subscriber
   */
  public static SendResults to(final AsyncResponse async) {
    return new SendResults(async);
  }

  private final AsyncResponse async;

  private SendResults(final AsyncResponse async) {
    this.async = async;
  }

  @Override
  public void onCompleted() {
    if (async.isSuspended()) {
      log.warn("got no data from operation - sending no content response");
      async.resume(Response.noContent().build());
    }
  }

  @Override
  public void onError(final Throwable error) {
    if (async.isSuspended()) {
      async.resume(error);
    } else {
      log.warn("must swallow error - response already resumed", error);
    }
  }

  @Override
  public void onNext(final StreamedResults results) {
    if (async.isSuspended()) {
      log.debug("resuming response on thread {}", Thread.currentThread());
      final Response response = Response
          .ok()
          .entity(results)
          .type(results.format())
          .build();
      async.resume(response);
    } else {
      log.warn("cannot send results - response already resumed");
      unsubscribe();
    }
  }
}
