package at.ac.univie.isc.asio;

import com.google.common.base.Optional;

/**
 * Root for all DatasetExceptions indicating errors on interaction with a dataset.
 * 
 * @author Chris Borckholder
 */
public abstract class DatasetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private Optional<DatasetOperation> failedOperation;

  protected DatasetException(final String prefix, final String message, final Throwable cause) {
    super(prefix + " : " + message, cause);
    failedOperation = Optional.absent();
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
