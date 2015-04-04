package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Eagerly add basic authentication headers to any intercepted request.
 * <strong>Note:</strong> Using preemptive basic auth reveals the credentials to
 * <strong>ANY</strong> targeted server!
 */
class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
  private final String header;

  public PreemptiveAuthInterceptor(final Credentials credentials) {
    header = encodeAsBasicAuthorizationHeader(credentials);
  }

  @Override
  public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
    request.addHeader(HttpHeaders.AUTHORIZATION, header);
  }

  private String encodeAsBasicAuthorizationHeader(final Credentials credentials) {
    final String usernameAndPassword =
        credentials.getUserPrincipal().getName() + ":" + credentials.getPassword();
    final String header = BaseEncoding.base64().encode(Payload.encodeUtf8(usernameAndPassword));
    return "Basic " + header;
  }
}
