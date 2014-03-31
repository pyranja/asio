package at.ac.univie.isc.asio.metadata;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 3/27/2014 ; Time: 2:42 PM
 */
public class RepositoryFailure extends RuntimeException {

  public RepositoryFailure(final String message, final Throwable cause) {
    super(message, cause);
  }
}
