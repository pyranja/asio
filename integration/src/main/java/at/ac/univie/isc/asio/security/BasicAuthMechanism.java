package at.ac.univie.isc.asio.security;

import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.apache.http.auth.Credentials;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Authenticate via basic authentication scheme, with fixed credentials.
 */
final class BasicAuthMechanism extends AuthMechanism {
  private final Credentials credentials;

  public BasicAuthMechanism(final Credentials secret) {
    this.credentials = requireNonNull(secret, "credentials");
  }

  @Override
  public boolean isAuthorizing() {
    // used credentials always grant full access rights
    return false;
  }

  @Override
  public URI configureUri(final URI uri, final String ignored) {
    return uri;
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String ignored) {
    final PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
    scheme.setUserName(credentials.getUserPrincipal().getName());
    scheme.setPassword(credentials.getPassword());
    return spec.setAuth(scheme);
  }
}
