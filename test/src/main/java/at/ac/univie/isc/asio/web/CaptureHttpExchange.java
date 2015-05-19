/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.web;

import at.ac.univie.isc.asio.io.CachedInputStream;
import at.ac.univie.isc.asio.io.TeeOutputStream;
import com.google.common.collect.Iterables;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base {@code HttpHandler} that captures all received {@code HttpExchange requests} in a list for
 * asserting on them. May be extended to add actual request processing, the base version always
 * responds 204 'no content' and an empty body.
 */
public final class CaptureHttpExchange implements HttpHandler {
  /**
   * A {@code HttpHandler} that always sends a 'no content' response.
   */
  public static CaptureHttpExchange create() {
    return new CaptureHttpExchange();
  }

  /**
   * A {@code HttpHandler} that sends the given response code and no content.
   * @param status fixed http response code
   */
  public static CaptureHttpExchange fixedStatus(final int status) {
    return create().delegateTo(HttpServer.fixedStatus(status));
  }

  public static class ExchangeAndContents {
    public final HttpExchange exchange;
    public final CachedInputStream requestBody;
    public final TeeOutputStream responseBody;

    public ExchangeAndContents(final HttpExchange exchange, final CachedInputStream requestBody, final TeeOutputStream responseBody) {
      this.exchange = exchange;
      this.requestBody = requestBody;
      this.responseBody = responseBody;
    }
  }

  private HttpHandler delegate = HttpServer.noContent();
  private final List<ExchangeAndContents> exchanges;

  protected CaptureHttpExchange() {
    exchanges = new CopyOnWriteArrayList<>();
  }

  @Override
  public final void handle(final HttpExchange httpExchange) throws IOException {
    final TeeOutputStream responseBody = TeeOutputStream.wrap(httpExchange.getResponseBody());
    final CachedInputStream requestBody = CachedInputStream.cache(httpExchange.getRequestBody());
    httpExchange.setStreams(requestBody, responseBody);
    exchanges.add(new ExchangeAndContents(httpExchange, requestBody, responseBody));
    try {
      delegate.handle(httpExchange);
    } finally {
      httpExchange.close();
    }
  }

  /**
   * Request processing will be delegated to given handler after capturing the exchange.
   */
  public CaptureHttpExchange delegateTo(final HttpHandler delegate) {
    this.delegate = delegate;
    return this;
  }

  /**
   * Get a single http exchange, fails if there are none or more than one exchange.
   */
  public final HttpExchange single() {
    return requireSingleCaptured().exchange;
  }

  /**
   * Get the single captured request body.
   */
  public final byte[] singleRequestBody() {
    return requireSingleCaptured().requestBody.cached();
  }

  private ExchangeAndContents requireSingleCaptured() {
    assert !exchanges.isEmpty() : "no http exchanges captures";
    assert exchanges.size() < 2 : "more than one http exchange captured";
    return Iterables.getOnlyElement(exchanges);
  }

  /**
   * All captured exchanges in the order received. May be empty.
   */
  public final List<ExchangeAndContents> getExchanges() {
    return exchanges;
  }
}
