package at.ac.univie.isc.asio;

import javax.annotation.Nonnull;

/**
 * A base class for exceptions, which represent failures due to a client error,
 * e.g. a request with illegal set of parameters.
 */
public abstract class InvalidUsage extends AsioError.Base {

  /**
   * A descriptive label of this error. Guaranteed to be non-null and non-empty.
   */
  @Nonnull
  public final String getLabel() {
    final String message = getMessage();
    return (message == null || message.isEmpty()) ? getClass().getSimpleName() : message;
  }

  // === subclass constructors =====================================================================

  /** Construct with given message */
  protected InvalidUsage(final String message) {
    super(message);
  }

  /** construct with message and cause */
  protected InvalidUsage(final String message, final Throwable cause) {
    super(message, cause);
  }
}
