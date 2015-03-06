package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Authenticate via basic authentication scheme, where the username is equal to the required role
 * and the configured, fixed password.
 */
final class BasicAuthMechanism extends AuthMechanism {
  private final String secret;

  public BasicAuthMechanism(final String secret) {
    this.secret = requireNonNull(secret, "secret");
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String role) {
    final PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
    scheme.setUserName(role);
    scheme.setPassword(this.secret);
    return spec.setAuth(scheme);
  }

  @Override
  public <CLIENT extends AbstractHttpClient> CLIENT configureClient(final CLIENT client, final String role) {
    final String credentials = BaseEncoding.base64().encode(Payload.encodeUtf8(role + ":" + secret));
    client.addRequestInterceptor(new PreemptiveAuthInterceptor(credentials));
    return client;
  }

  private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
    private final String credentials;

    public PreemptiveAuthInterceptor(final String credentials) {
      this.credentials = credentials;
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
    }
  }
}
