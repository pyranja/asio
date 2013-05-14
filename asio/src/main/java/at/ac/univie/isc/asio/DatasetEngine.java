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
	 * Execute the given query.
	 * 
	 * @param query
	 *            to be executed
	 * @return future holding the result data or errors encountered
	 */
	ListenableFuture<InputSupplier<InputStream>> submit(String query);

	/**
	 * @return all {@link SerializationFormat}s supported by this engine.
	 */
	Set<SerializationFormat> supportedFormats();
}
