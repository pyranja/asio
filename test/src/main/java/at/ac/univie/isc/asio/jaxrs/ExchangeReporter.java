package at.ac.univie.isc.asio.jaxrs;


import com.google.common.base.Charsets;

import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Describe a JAX-RS HTTP exchange.
 */
@NotThreadSafe
final class ExchangeReporter {
  public static ExchangeReporter create() {
    return new ExchangeReporter();
  }

  private final StringBuilder requestReport =
      new StringBuilder("NO REQUEST CAPTURED").append(System.lineSeparator());
  private final StringBuilder responseReport =
      new StringBuilder("NO RESPONSE CAPTURED").append(System.lineSeparator());

  private ExchangeReporter() {
  }

  public ExchangeReporter update(ClientRequestContext request) {
    requireNonNull(request);
    requestReport.setLength(0);
    requestReport
        .append(requestLine(request))
        .append(headerLine(request.getStringHeaders()));
    return this;
  }

  public ExchangeReporter update(ClientResponseContext response) {
    requireNonNull(response);
    responseReport.setLength(0);
    responseReport
        .append(responseLine(response))
        .append(headerLine(response.getHeaders()));
    return this;
  }

  public ExchangeReporter responseBody(final byte[] data) {
    attach(responseReport, data);
    return this;
  }

  public ExchangeReporter requestBody(final byte[] data) {
    attach(requestReport, data);
    return this;
  }

  private void attach(final StringBuilder target, final byte[] data) {
    requireNonNull(data);
    final String text = new String(data, Charsets.UTF_8);
    target.append(bodyLine(text));
  }

  private static final String INTRO = "===== HTTP EXCHANGE =====";
  private static final String HLINE = "-------------------------";
  private static final String OUTRO = "=========================";

  public String format() {
    return
        INTRO + System.lineSeparator() + requestReport +
            HLINE + System.lineSeparator() + responseReport +
            OUTRO;
  }

  private String requestLine(final ClientRequestContext request) {
    return String.format(Locale.ENGLISH, " REQUEST %s %s%n", request.getMethod(), request.getUri());
  }

  private String responseLine(final ClientResponseContext response) {
    final Response.StatusType status = response.getStatusInfo();
    return String.format(Locale.ENGLISH, " RESPONSE %s %s %s%n", status.getFamily(), status.getStatusCode(), status.getReasonPhrase());
  }

  private String headerLine(final MultivaluedMap<String, String> headers) {
    return String.format(Locale.ENGLISH, " HEADER %s%n", headers);
  }

  private String bodyLine(final String body) {
    return String.format(Locale.ENGLISH, " BODY %s EOF%n", body);
  }

}
