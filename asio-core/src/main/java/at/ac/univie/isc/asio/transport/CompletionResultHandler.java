package at.ac.univie.isc.asio.transport;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;

import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Await completion of operation before providing access to the result data.
 * 
 * @author Chris Borckholder
 */
public final class CompletionResultHandler implements ResultHandler {

	private final Buffer buffer;
	private final MediaType contentType;
	private final SettableFuture<Result> future;

	CompletionResultHandler(final Buffer buffer, final MediaType contentType) {
		this.buffer = buffer;
		this.contentType = contentType;
		future = SettableFuture.create();
	}

	@Override
	public OutputStream getOutput() throws IOException {
		return buffer.getOutput();
	}

	@Override
	public void fail(final DatasetException cause) {
		checkNotNull(cause, "fail must include a cause");
		future.setException(cause);
	}

	@Override
	public void complete() {
		future.set(new Result() {
			@Override
			public InputStream getInput() throws IOException {
				return buffer.getInput();
			}

			@Override
			public MediaType mediaType() {
				return contentType;
			}

		});
	}

	@Override
	public ListenableFuture<Result> asFutureResult() {
		return future;
	}

}
