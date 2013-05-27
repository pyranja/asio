package at.ac.univie.isc.asio.frontend;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.Result;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Await {@link Result} completion and resume the JAXRS response after
 * completion.
 * 
 * @author Chris Borckholder
 */
public class AsyncProcessor {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(AsyncProcessor.class);

	private final ExecutorService exec;
	private final VariantConverter converter;

	AsyncProcessor(final ExecutorService exec, final VariantConverter converter) {
		super();
		this.exec = exec;
		this.converter = converter;
	}

	/**
	 * Resume the given {@link AsyncResponse} when the given {@link Result}
	 * future completes.
	 * 
	 * @param future
	 *            pending results
	 * @param response
	 *            to be continued
	 */
	public void handle(final ListenableFuture<Result> future,
			final AsyncResponse response) {
		final FutureCallback<Result> callback = new FutureCallback<Result>() {
			@Override
			public void onSuccess(final Result result) {
				try {
					final Response success = successResponse(result);
					log.info("operation completed successfully");
					response.resume(success);
				} catch (final IOException e) {
					log.error("streaming result data failed", e);
					response.resume(wrapFailure(e));
				}
			}

			@Override
			public void onFailure(final Throwable t) {
				log.info("operation failed", t);
				response.resume(wrapFailure(t));
			}
		};
		Futures.addCallback(future, callback, exec);
	}

	private Response successResponse(final Result result) throws IOException {
		final MediaType contentType = converter.asContentType(result
				.mediaType());
		return Response.ok(result.getInput(), contentType).build();
	}

	private DatasetException wrapFailure(final Throwable t) {
		if (t instanceof DatasetException) {
			return (DatasetException) t;
		} else {
			return new DatasetFailureException(t);
		}
	}
}
