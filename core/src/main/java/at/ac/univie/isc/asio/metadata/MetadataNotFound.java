package at.ac.univie.isc.asio.metadata;

/**
 * Thrown if no metadata for a given dataset identifier was found.
 */
public final class MetadataNotFound extends RuntimeException {
  public MetadataNotFound(final String message, final Throwable cause) {
    super(message, cause);
  }
}
