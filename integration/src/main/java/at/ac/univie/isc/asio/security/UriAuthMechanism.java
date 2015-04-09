package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.web.WebTools;
import com.jayway.restassured.builder.RequestSpecBuilder;

import java.net.URI;

/**
 * Authorize requests by injecting the role into the request uri.
 */
final class UriAuthMechanism extends AuthMechanism {
  @Override
  public boolean isAuthorizing() {
    return true;
  }

  @Override
  public URI configureUri(final URI uri, final String role) {
    return uri.resolve(WebTools.ensureDirectoryPath(URI.create(role)));
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String ignored) {
    return spec;
  }
}
