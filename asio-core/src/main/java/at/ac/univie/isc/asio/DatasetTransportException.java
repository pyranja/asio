package at.ac.univie.isc.asio;

/**
 * Indicate a failure in the asio transport infrastructure, e.g. while streaming result data.
 * 
 * @author Chris Borckholder
 */
public class DatasetTransportException extends DatasetException {

  private static final long serialVersionUID = 1L;

  private static final String PREFIX = "dataset transport error";

  public DatasetTransportException(final Throwable cause) {
    super(PREFIX, cause.getLocalizedMessage(), cause);
  }
}
