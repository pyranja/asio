package at.ac.univie.isc.asio.transport;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import at.ac.univie.isc.asio.ResultHandler;

import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Await completion of operation before providing access to the result data.
 * 
 * @author Chris Borckholder
 */
public final class CompletionResultHandler implements ResultHandler {

	private final Buffer buffer;
	private final SettableFuture<InputSupplier<InputStream>> future;

	CompletionResultHandler(final Buffer buffer) {
		this.buffer = buffer;
		future = SettableFuture.create();
	}

	@Override
	public OutputStream getOutput() throws IOException {
		return buffer.getOutput();
	}

	@Override
	public void fail(final Exception cause) {
		checkNotNull(cause, "fail must include a cause");
		future.setException(cause);
	}

	@Override
	public void complete() {
		future.set(buffer);
	}

	@Override
	public ListenableFuture<InputSupplier<InputStream>> asFutureResult() {
		return future;
	}

}
