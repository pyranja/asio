package at.ac.univie.isc.asio;

/**
 * Indicate incorrect usage of a dataset, e.g. bad query syntax or missing
 * parameters.
 * 
 * @author Chris Borckholder
 */
public class DatasetUsageException extends DatasetException {

	private static final long serialVersionUID = 1L;

	private static final String PREFIX = "invalid usage of dataset";

	public DatasetUsageException(final Throwable cause) {
		super(PREFIX, cause.getLocalizedMessage(), cause);
	}

	public DatasetUsageException(final String message) {
		super(PREFIX, message, null);
	}

}
