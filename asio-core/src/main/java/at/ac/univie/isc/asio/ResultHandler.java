package at.ac.univie.isc.asio;

import java.io.IOException;
import java.io.OutputStream;

import com.google.common.io.OutputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provide an {@link OutputStream} to {@link DatasetEngine}s for result delivery
 * and enable asynchronous completion or failure notification through callbacks.
 * 
 * @author Chris Borckholder
 */
public interface ResultHandler extends OutputSupplier<OutputStream> {

	/**
	 * Retrieve the {@link OutputStream} which can be used to deliver this
	 * result data.
	 * <p>
	 * Each invocation will return the <strong>same</strong> stream. The
	 * returned stream will <strong>not</strong> be thread-safe.
	 * </p>
	 * 
	 * @return OutputStream delivery stream
	 * @throws IOException
	 *             if stream retrieval failed
	 */
	@Override
	OutputStream getOutput() throws IOException;

	/**
	 * Mark the operation as failed due to the given Exception.
	 * 
	 * @param cause
	 *            of failure
	 */
	void fail(DatasetException cause);

	/**
	 * Mark the operation as complete. No more result data will be written to
	 * this result.
	 */
	void complete();

	/**
	 * Return a {@link ListenableFuture} which will provide access to the result
	 * data written to the stream acquired by {@link #getOutput()}.
	 * 
	 * @return a future that represents the delivery of this result.
	 */
	ListenableFuture<Result> asFutureResult();
}
