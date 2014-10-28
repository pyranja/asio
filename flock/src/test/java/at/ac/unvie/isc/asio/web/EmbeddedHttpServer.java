package at.ac.unvie.isc.asio.web;

import at.ac.univie.isc.asio.Payload;
import at.ac.univie.isc.asio.jaxrs.CachedInputStream;
import at.ac.univie.isc.asio.jaxrs.TeeOutputStream;
import at.ac.univie.isc.asio.tool.Pretty;
import at.ac.unvie.isc.asio.junit.ReportCollector;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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

import static at.ac.univie.isc.asio.tool.Pretty.justify;
import static java.lang.String.format;

/**
 * Provide a lightweight, embedded {@link com.sun.net.httpserver.HttpServer http server} listening
 * to a random, free port on localhost.
 */
public final class EmbeddedHttpServer extends ExternalResource implements ReportCollector.Report {

  /**
   * @param label describing this server
   * @return the server
   */
  public static EmbeddedHttpServer create(final String label) {
    return new EmbeddedHttpServer(label);
  }

  private static final Splitter.MapSplitter PARAMETER_PARSER =
      Splitter.on('&').trimResults().omitEmptyStrings().withKeyValueSeparator('=');

  private static final ThreadFactory THREAD_FACTORY =
      new ThreadFactoryBuilder().setNameFormat("embedded-http-worker-%d").build();

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
    final String query = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.name());
    params.putAll(PARAMETER_PARSER.split(query));
    if (isForm(exchange)) {
      final String rawBody = Payload.asString(ByteStreams.toByteArray(exchange.getRequestBody()));
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

  private final HttpServer server;
  private final String label;
  private final ExecutorService exec = Executors.newCachedThreadPool(THREAD_FACTORY);
  private final StringBuilder reporter = new StringBuilder();
  private boolean logging = false;

  private EmbeddedHttpServer(final String label) {
    this.label = label;
    try {
      server = HttpServer.create();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Contexts {@link #with(String, com.sun.net.httpserver.HttpHandler) registered} after enabling,
   * will log HTTP requests and responses to {@code System.out}.
   *
   * @return this server
   */
  public EmbeddedHttpServer enableLogging() {
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
  public EmbeddedHttpServer with(final String path, final HttpHandler handler) {
    final List<Filter> filters = server.createContext(path, handler).getFilters();
    if (logging) { filters.add(new LogFilter(System.out)); }
    filters.add(new LogFilter(reporter));
    filters.add(new ErrorFilter());
    return this;
  }

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    final String labelLine = justify(Pretty.format(" <%s> ", label), 75, '#');
    return sink
        .append(labelLine).append(System.lineSeparator())
        .append(reporter)
        .append(labelLine).append(System.lineSeparator());
  }

  @Override
  protected void before() throws Throwable {
    final InetAddress localhost = InetAddress.getByName(null);
    server.bind(new InetSocketAddress(localhost, 0), 0);
    server.start();
  }

  @Override
  protected void after() {
    try {
      server.stop(0);
    } finally {
      exec.shutdown();
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("logging", logging)
        .add("label", label)
        .toString();
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
            .received(exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRequestHeaders())
            .withRequestBody(requestBody.cached());
        chain.doFilter(exchange);
        report
            .sent(exchange.getResponseCode(), exchange.getResponseHeaders())
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
