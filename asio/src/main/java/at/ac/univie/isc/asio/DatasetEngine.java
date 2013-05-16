package at.ac.univie.isc.asio;

import java.io.InputStream;
import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Accept and execute DatasetOperations. Execution may be asynchronous.
 * 
 * @author Chris Borckholder
 */
public interface DatasetEngine {

	/**
	 * Execute the given {@link DatasetOperation operation}. The returned future
	 * must hold the result data of the operation in a format that is compatible
	 * to the {@link SerializationFormat format} given in the
	 * {@link DatasetOperation#format() operation}.
	 * <p>
	 * Implementations should generally not throw exceptions if the operation is
	 * unacceptable or an error occurs during execution, but set the appropriate
	 * error state on the returned future.
	 * </p>
	 * 
	 * @param operation
	 *            to be executed
	 * @return future holding the result data or errors encountered
	 */
	ListenableFuture<InputSupplier<InputStream>> submit(
			DatasetOperation operation);

	/**
	 * @return all {@link SerializationFormat}s supported by this engine.
	 */
	Set<SerializationFormat> supportedFormats();
}
