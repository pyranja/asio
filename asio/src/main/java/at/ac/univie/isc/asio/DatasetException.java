package at.ac.univie.isc.asio;

/**
 * Root for all DatasetExceptions indicating errors on interaction with a
 * dataset.
 * 
 * @author Chris Borckholder
 */
// XXX add dataset informations -> id ?
public abstract class DatasetException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected DatasetException(final String prefix, final String message,
			final Throwable cause) {
		super(prefix + " : " + message, cause);
	}

}