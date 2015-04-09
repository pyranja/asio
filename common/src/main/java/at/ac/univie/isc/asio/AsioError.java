package at.ac.univie.isc.asio;

/**
 * Mark all exceptions originating from the asio runtime.
 */
public interface AsioError {

  /**
   * A detailed description of the cause of the error. When marking an exception class,
   * this method is already implemented, as it is inherited from {@code Throwable}.
   *
   * @return the description of the error
   * @see Throwable#getMessage()
   */
  String getMessage();

  /**
   * Simple base class for asio errors.
   */
  abstract class Base extends RuntimeException implements AsioError {
    protected Base(final String message) {
      this(message, null);
    }

    protected Base(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
