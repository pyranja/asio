package at.ac.univie.isc.asio.security;

import com.jayway.restassured.builder.RequestSpecBuilder;

import java.net.URI;

/**
 * This mechanism does not perform authentication or authorization.
 */
final class NullAuthMechanism extends AuthMechanism {
  @Override
  public boolean isAuthorizing() {
    return false;
  }

  @Override
  public URI configureUri(final URI uri, final String ignored) {
    return uri;
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String ignored) {
    return spec;
  }
}
