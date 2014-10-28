package at.ac.univie.isc.asio;

/**
 * Indicate an internal error caused by the used dataset on processing a request, e.g. a jdbc
 * connection problem.
 * 
 * @author Chris Borckholder
 */
public class DatasetFailureException extends DatasetException {

  private static final long serialVersionUID = 1L;

  private static final String PREFIX = "internal dataset error";

  public DatasetFailureException(final Throwable cause) {
    super(PREFIX, cause.getLocalizedMessage(), cause);
  }

  public DatasetFailureException(final String message, final Throwable cause) {
    super(PREFIX, message, cause);
  }
}
