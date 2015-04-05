package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.tool.Pretty;

import java.net.URI;

/**
 * Thrown if the metadata repository cannot be reached or returned illegal responses.
 */
public final class RepositoryFailure extends RuntimeException {
  public RepositoryFailure(final String message, final URI endpoint, final Throwable cause) {
    super(Pretty.format("failed to communicate with metadata repository at %s - %s", endpoint, message), cause);
  }
}
