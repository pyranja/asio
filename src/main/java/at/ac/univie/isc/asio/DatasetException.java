package at.ac.univie.isc.asio;

import com.google.common.base.Optional;

import java.util.Locale;

/**
 * Root for all DatasetExceptions indicating errors on interaction with a dataset.
 *
 * @author Chris Borckholder
 */
public abstract class DatasetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String MESSAGE_TEMPLATE = "%s : %s";

  private Optional<DatasetOperation> failedOperation;

  protected DatasetException(final String prefix, final String message, final Throwable cause) {
    super(createMessage(prefix, message, cause), cause);
    failedOperation = Optional.absent();
  }

  // FIXME : hacked to allow call in constructor
  private static String createMessage(final String prefix, final String message, final Throwable cause) {
    final String reason = message == null
        ? cause.toString()
        : message;
    return String.format(Locale.ENGLISH, MESSAGE_TEMPLATE, prefix, reason);
  }

  /**
   * @return the {@link DatasetOperation} that failed if it is known.
   */
  public Optional<DatasetOperation> failedOperation() {
    return failedOperation;
  }

  /**
   * Attach the operation information to this exception
   *
   * @param failedOperation which encountered the error
   */
  public void setFailedOperation(final DatasetOperation failedOperation) {
    this.failedOperation = Optional.fromNullable(failedOperation);
  }
}
