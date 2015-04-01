package at.ac.univie.isc.asio;

/**
 * A marker interface for exceptions, which represent failures due to a client error,
 * e.g. a request with illegal set of parameters.
 */
public interface InvalidUsage {
  /**
   * A detailed description of the cause of the error.
   *
   * @return the description of the error
   * @see Throwable#getMessage() when marking an exception, this is already implemented
   */
  String getMessage();
}
