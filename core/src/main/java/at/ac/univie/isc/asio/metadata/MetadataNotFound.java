package at.ac.univie.isc.asio.metadata;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 3/27/2014 ; Time: 3:54 PM
 */
public class MetadataNotFound extends RuntimeException {

  public MetadataNotFound(final String message, final Throwable cause) {
    super(message, cause);
  }
}
