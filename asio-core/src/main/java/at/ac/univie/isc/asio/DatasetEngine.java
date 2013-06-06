package at.ac.univie.isc.asio;

import java.security.Principal;
import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

/**
 * Accept and execute DatasetOperations. Execution may be asynchronous.
 * 
 * @author Chris Borckholder
 */
public interface DatasetEngine {

	/**
	 * Execute the given {@link DatasetOperation operation}. The results must be
	 * delivered through the given {@link ResultHandler handler} in a format
	 * that is compatible to the {@link SerializationFormat format} given in the
	 * {@link DatasetOperation#format() operation}.
	 * 
	 * <p>
	 * Implementations should generally not throw exceptions if the operation is
	 * unacceptable or an error occurs during execution, but set the appropriate
	 * error state on the {@link ResultHandler#fail(DatasetException) handler}.
	 * </p>
	 * 
	 * @param operation
	 *            to be executed
	 * @param handler
	 *            callback for result progress, state and delivery
	 * @param principal
	 *            optional authentication and/or authorization informations
	 *            about the subject that executes the operation (e.g. a logged
	 *            in user)
	 */
	void submit(DatasetOperation operation, ResultHandler handler,
			Principal principal);

	/**
	 * @return all {@link SerializationFormat result formats} supported by this
	 *         engine.
	 */
	Set<SerializationFormat> supportedFormats();
}
