package at.ac.univie.isc.asio.security;

import java.net.URI;

/**
 * Authorize requests by injecting the role into the request uri.
 */
final class UriAuthMechanism extends AuthMechanism {
  @Override
  public URI configureUri(final URI uri, final String role) {
    return uri.resolve(role + '/');
  }
}
