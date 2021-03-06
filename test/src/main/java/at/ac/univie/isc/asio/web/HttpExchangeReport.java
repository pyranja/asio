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
import at.ac.univie.isc.asio.junit.Interactions;
import com.google.common.base.Joiner;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Capture and format details of a HTTP request/response exchange.
 */
public final class HttpExchangeReport implements Interactions.Report {
  public static HttpExchangeReport create() {
    return new HttpExchangeReport("unknown");
  }

  public static HttpExchangeReport named(final String name) {
    return new HttpExchangeReport(name);
  }

  private static final Joiner.MapJoiner HEADER_JOINER =
      Joiner.on(',').withKeyValueSeparator("=").useForNull(Objects.toString(null));

  private static final String INTRO = Pretty.justify(" START HTTP EXCHANGE ", 75, '=');
  private static final String HLINE = Pretty.justify("", 75, '-');
  private static final String OUTRO = Pretty.justify(" END HTTP EXCHANGE ", 75, '=');

  private final StringBuilder requestReport =
      new StringBuilder(" NO REQUEST CAPTURED").append(System.lineSeparator());
  private final StringBuilder responseReport =
      new StringBuilder(" NO RESPONSE CAPTURED").append(System.lineSeparator());

  private Throwable error = null;
  private final String label;

  private HttpExchangeReport(final String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return "HttpExchangeReport{" + label + '}';
  }

  /**
   * @return formatted summary of captured data
   */
  public String format() {
    try {
      return appendTo(new StringBuilder()).toString();
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Append a summary of captured data to the given sink.
   *
   * @param sink to write to
   * @return the sink
   * @throws IOException on any error
   */
  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    sink.append(INTRO).append(System.lineSeparator()).append(requestReport)
        .append(HLINE).append(System.lineSeparator()).append(responseReport);
    if (error != null) {
      sink.append(HLINE).append(System.lineSeparator()).append(error.getMessage()).append(System.lineSeparator());
    }
    sink.append(OUTRO).append(System.lineSeparator());
    return sink;
  }

  /**
   * Capture request data.
   *
   * @param method  request method
   * @param uri     request uri
   * @param headers request headers
   * @return this report
   */
  public HttpExchangeReport captureRequest(final String method, final URI uri, final Map<?, ?> headers) {
    requestReport.setLength(0);
    requestReport
        .append(Pretty.format(" REQUEST %s %s%n", method, uri))
        .append(Pretty.format(" HEADER {%s}%n", HEADER_JOINER.join(headers)));
    return this;
  }

  /**
   * Attach a request body.
   *
   * @param data utf-8 encoded body
   * @return this report
   */
  public HttpExchangeReport withRequestBody(final byte[] data) {
    attachBody(requestReport, data);
    return this;
  }

  /**
   * Capture response data.
   *
   * @param code    response code
   * @param headers response headers
   * @return this report
   */
  public HttpExchangeReport captureResponse(final int code, final Map<?, ?> headers) {
    responseReport.setLength(0);
    responseReport
        .append(Pretty.format(" RESPONSE %d%n", code))
        .append(Pretty.format(" HEADER {%s}%n", HEADER_JOINER.join(headers)));
    return this;
  }

  /**
   * Attach a response body.
   *
   * @param data utf-8 encoded body
   * @return this report
   */
  public HttpExchangeReport withResponseBody(final byte[] data) {
    attachBody(responseReport, data);
    return this;
  }

  /**
   * Attach an internal error.
   *
   * @param error which occurred
   * @return this report
   */
  public HttpExchangeReport failure(final Throwable error) {
    this.error = requireNonNull(error);
    return this;
  }

  private static void attachBody(final StringBuilder target, final byte[] data) {
    requireNonNull(data);
    final String text = new String(data, StandardCharsets.UTF_8);
    target.append(Pretty.format(" BODY %s EOF%n", text));
  }
}
