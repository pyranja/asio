package at.ac.univie.isc.asio.ogsadai;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.UserExceptionMarker;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;

import com.google.common.base.Throwables;

/**
 * Translate {@link DAIException}s to corresponding {@link DatasetExeption}s.
 * 
 * @author Chris Borckholder
 */
public class DaiExceptionTranslator {

	public DatasetException translate(final Exception error) {
		if (error instanceof DatasetException) {
			return (DatasetException) error;
		}
		final Throwable root = Throwables.getRootCause(error);
		if (error instanceof UserExceptionMarker) {
			return new DatasetUsageException(root.getMessage(), error);
		} else {
			return new DatasetFailureException(root.getMessage(), error);
		}
	}

}
