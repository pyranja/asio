package at.ac.univie.isc.asio.ogsadai;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.transport.FileResultRepository;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Defer DatasetOperations to an OGSADAI server instance.
 * 
 * @author Chris Borckholder
 */
public final class OgsadaiEngine implements DatasetEngine {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(OgsadaiEngine.class);

	private final OgsadaiAdapter ogsadai;
	private final FileResultRepository results;
	private final WorkflowComposer composer;

	OgsadaiEngine(final OgsadaiAdapter ogsadai,
			final FileResultRepository results, final WorkflowComposer composer) {
		super();
		this.ogsadai = ogsadai;
		this.results = results;
		this.composer = composer;
	}

	/**
	 * @return all {@link OgsadaiFormats formats} supported by OGSADAI.
	 */
	@Override
	public Set<SerializationFormat> supportedFormats() {
		return OgsadaiFormats.asSet();
	}

	/**
	 * Create and invoke an OGSADAI {@link Workflow} which executes the query
	 * asynchronously and provide the result data through the returned future.
	 * This method will not throw exceptions but sets the execution state of the
	 * returned {@link ListenableFuture} instead.
	 * 
	 * @param query
	 *            to be executed
	 * @return future holding result data or execution error
	 */
	@Override
	public ListenableFuture<Result> submit(final DatasetOperation operation) {
		final ResultHandler handler = results.newHandler(operation.format());
		ogsadai.register(operation.id(), handler);
		log.trace("[{}] registered handler with exchanger", operation);
		final Workflow workflow = composer.createFrom(operation);
		log.trace("[{}] using workflow :\n{}", operation, workflow);
		final CompletionCallback callback = delegateTo(handler);
		try {
			ogsadai.invoke(operation.id(), workflow, callback);
		} catch (final DatasetException cause) {
			// clean up exchange
			ogsadai.revokeSupplier(operation.id());
			handler.fail(cause);
		}
		return handler.asFutureResult();
	}

	/**
	 * Create a CompletionCallback that delegates to the given ResultHandler.
	 */
	private CompletionCallback delegateTo(final ResultHandler handler) {
		return new CompletionCallback() {

			@Override
			public void complete() {
				handler.complete();
			}

			@Override
			public void fail(final Exception cause) {
				handler.fail(cause);
			}

		};
	}
}
