package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.io.OutputStream;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * The future of this result handler will be available as soon as either
 * <ul>
 * <li>{@link #complete()} is called</li>
 * <li>{@link #fail(DatasetException)} is called</li>
 * <li>the buffer stream is {@link #getOutput() retrieved}</li>
 * </ul>
 * 
 * @author Chris Borckholder
 */
public class StreamResultHandler implements ResultHandler {

	private final OutputStream buffer;
	private final Result result;

	private final SettableFuture<Result> future;

	StreamResultHandler(final OutputStream buffer, final Result result) {
		future = SettableFuture.create();
		this.result = result;
		this.buffer = buffer;
	}

	@Override
	public OutputStream getOutput() throws IOException {
		future.set(result); // assumes successful execution
		return buffer;
	}

	@Override
	public void fail(final DatasetException cause) {
		future.setException(cause);
	}

	@Override
	public void complete() {
		future.set(result);
	}

	@Override
	public ListenableFuture<Result> asFutureResult() {
		return future;
	}
}
