package at.ac.univie.isc.asio;

/**
 * Accept notifications about the progress of a dataset operation.
 * 
 * @author Chris Borckholder
 */
public interface DatasetOperationTracker {

	/**
	 * operation is processing and generated first results.
	 */
	void receiving();

	/**
	 * operation completed successfully.
	 */
	void complete();

	/**
	 * operation failed due to the given cause.
	 * 
	 * @param cause
	 *            of failure
	 */
	void fail(Exception cause);
}
