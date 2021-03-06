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

import at.ac.univie.isc.asio.Pretty;
import at.ac.univie.isc.asio.Unchecked;
import at.ac.univie.isc.asio.io.CachedInputStream;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.io.TeeOutputStream;
import at.ac.univie.isc.asio.junit.Interactions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.lang.String.format;

/**
 * Provide a lightweight, embedded {@link com.sun.net.httpserver.HttpServer http server} listening
 * to a random, free port on localhost.
 */
public final class HttpServer extends ExternalResource implements Interactions.Report, AutoCloseable {

  // === tools =====================================================================================

  /** send 204 status and no response body */
  public static HttpHandler noContent() {
    return new HttpHandler() {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpStatus.SC_NO_CONTENT, -1);
      }
    };
  }

  /** send given http status and no response body */
  public static HttpHandler fixedStatus(final int status) {
    return new HttpHandler() {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(status, -1);
      }
    };
  }

  /** send given http status and raw data as response body */
  public static HttpHandler staticContent(final int status, final String mime, final byte[] payload) {
    return new HttpHandler() {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, mime);
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.getResponseBody().flush();
      }
    };
  }

  /** send given http status and text as json content */
  public static HttpHandler jsonContent(final int status, final String json) {
    return staticContent(status, "application/json", Payload.encodeUtf8(json));
  }

  /**
   * Attempt to parse request parameters from the query string and the request body in case of a
   * form submission.
   *
   * @param exchange http exchange
   * @return parsed parameters as map
   * @throws IOException on any errors
   */
  public static Map<String, String> parseParameters(final HttpExchange exchange) throws IOException {
    final ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
    final String rawQuery = exchange.getRequestURI().getQuery();
    if (rawQuery != null) { // null if no query string present
      final String query = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.name());
      params.putAll(PARAMETER_PARSER.split(query));
    }
    if (isForm(exchange)) {
      final String rawBody = Payload.decodeUtf8(ByteStreams.toByteArray(exchange.getRequestBody()));
      final String form = URLDecoder.decode(rawBody, StandardCharsets.UTF_8.name());
      params.putAll(PARAMETER_PARSER.split(form));
    }
    return params.build();
  }

  /**
   * @param exchange http exchange
   * @return true if request is a form submission
   */
  public static boolean isForm(final HttpExchange exchange) {
    final String contentType = exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
    final String method = exchange.getRequestMethod();
    return "POST".equalsIgnoreCase(method)
        && "application/x-www-form-urlencoded".equalsIgnoreCase(contentType);
  }

  // === http server implementation ================================================================

  private static final Splitter.MapSplitter PARAMETER_PARSER =
      Splitter.on('&').trimResults().omitEmptyStrings().withKeyValueSeparator('=');
  private static final int THREAD_COUNT = 2;

  private static final ThreadFactory THREAD_FACTORY =
      new ThreadFactoryBuilder().setNameFormat("embedded-http-worker-%d").build();

  private final com.sun.net.httpserver.HttpServer server;
  private final String label;
  private final ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT, THREAD_FACTORY);
  private final StringBuilder capturedExchanges = new StringBuilder();
  private boolean logging = false;

  private HttpServer(final String label) {
    this.label = label;
    try {
      server = com.sun.net.httpserver.HttpServer.create();
      server.setExecutor(exec);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * factory method
   */
  public static HttpServer create(final String label) {
    return new HttpServer(label);
  }

  /**
   * Contexts {@link #with(String, com.sun.net.httpserver.HttpHandler) registered} after enabling,
   * will log HTTP requests and responses to {@code System.out}.
   *
   * @return this server
   */
  public HttpServer enableLogging() {
    this.logging = true;
    return this;
  }

  /**
   * @return the address this server is listening to
   */
  public URI address() {
    return URI.create(format(Locale.ENGLISH, "http://localhost:%d/", server.getAddress().getPort()));
  }

  /**
   * Add a path to this server.
   *
   * @param path    path of new path
   * @param handler request handling delegate
   * @return this server instance
   */
  public HttpServer with(final String path, final HttpHandler handler) {
    final List<Filter> filters = server.createContext(path, handler).getFilters();
    if (logging) { filters.add(new LogFilter(System.out)); }
    filters.add(new LogFilter(capturedExchanges));
    filters.add(new ErrorFilter());
    return this;
  }

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    sink.append(capturedExchanges);
    capturedExchanges.setLength(0);
    return sink;
  }

  public HttpServer start(final int port) {
    log(Pretty.format("starting on port %d", port));
    try {
      final InetAddress localhost = InetAddress.getByName(null);
      server.bind(new InetSocketAddress(localhost, port), 0);
      server.start();
    } catch (IOException e) {
      throw new Unchecked.UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public void close() {
    log("shutting down...");
    try {
      server.stop(0);
    } finally {
      exec.shutdown();
    }
  }

  @Override
  protected void before() throws Throwable {
    start(0);
  }

  @Override
  protected void after() {
    close();
  }

  private void log(final String message) {
    if (logging) {
      System.out.println("[" + label + "] " + message);
    }
  }

  @Override
  public String toString() {
    return "HttpServer{" + label + '}';
  }

  /**
   * Log all http exchanges.
   */
  static final class LogFilter extends Filter {
    private final Appendable log;

    LogFilter(final Appendable log) {
      this.log = log;
    }

    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
      final HttpExchangeReport report = HttpExchangeReport.create();
      try {
        final TeeOutputStream responseBody = TeeOutputStream.wrap(exchange.getResponseBody());
        final CachedInputStream requestBody = CachedInputStream.cache(exchange.getRequestBody());
        exchange.setStreams(requestBody, responseBody);
        report
            .captureRequest(exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRequestHeaders())
            .withRequestBody(requestBody.cached());
        chain.doFilter(exchange);
        report
            .captureResponse(exchange.getResponseCode(), exchange.getResponseHeaders())
            .withResponseBody(responseBody.captured());
      } catch (Exception e) {
        report.failure(e);
        Throwables.propagateIfInstanceOf(e, IOException.class);
        throw Throwables.propagate(e);
      } finally {
        report.appendTo(log);
      }
    }

    @Override
    public String description() {
      return "log filter";
    }
  }


  /**
   * Wrap a {@code HttpHandler}, report any uncaught errors and attempt to send a 500 response.
   */
  static final class ErrorFilter extends Filter {
    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
      try {
        chain.doFilter(exchange);
      } catch (final Exception e) {
        exchange.sendResponseHeaders(HttpStatus.SC_INTERNAL_SERVER_ERROR, -1);
      } finally {
        exchange.close();
      }
    }

    @Override
    public String description() {
      return "error filter";
    }
  }
}
