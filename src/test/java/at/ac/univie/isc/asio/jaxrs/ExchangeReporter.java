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
public final class ExchangeReporter {
  public static ExchangeReporter create() {
    return new ExchangeReporter();
  }

  private final StringBuilder report = new StringBuilder("NO EXCHANGE CAPTURED");

  private ExchangeReporter() {}

  public ExchangeReporter update(ClientRequestContext request) {
    requireNonNull(request);
    report.setLength(0);
    report
        .append(requestLine(request))
        .append(headerLine(request.getStringHeaders()));
    return this;
  }

  public ExchangeReporter update(ClientResponseContext response) {
    requireNonNull(response);
    report
        .append(responseLine(response))
        .append(headerLine(response.getHeaders()));
    return this;
  }

  public ExchangeReporter with(final byte[] data) {
    requireNonNull(data);
    final String text = new String(data, Charsets.UTF_8);
    report.append(bodyLine(text));
    return this;
  }

  public String format() {
    return report.toString();
  }

  private String requestLine(final ClientRequestContext request) {
    return String.format(Locale.ENGLISH, "REQUEST %s %s%n", request.getMethod(), request.getUri());
  }

  private String responseLine(final ClientResponseContext response) {
    final Response.StatusType status = response.getStatusInfo();
    return String.format(Locale.ENGLISH, "RESPONSE %s %s %s%n", status.getFamily(), status.getStatusCode(), status.getReasonPhrase());
  }

  private String headerLine(final MultivaluedMap<String, String> headers) {
    return String.format(Locale.ENGLISH, "HEADER %s%n", headers);
  }

  private String bodyLine(final String body) {
    return String.format(Locale.ENGLISH, "BODY %s END%n", body);
  }
}
