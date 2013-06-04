package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.core.Request;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.ResultRepository;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Enhances a {@link DatasetEngine} with content negotiation functionality and
 * handles result management.
 * 
 * @author Chris Borckholder
 */
public class EngineAdapter {

	/**
	 * convenience factory method for testing
	 */
	@VisibleForTesting
	static EngineAdapter adapt(final DatasetEngine engine) {
		final FormatSelector selector = new FormatSelector(
				engine.supportedFormats(), new VariantConverter());
		// TODO hacked for tests
		final ResultRepository repo = new ResultRepository() {
			@Override
			public ResultHandler newHandlerFor(final DatasetOperation operation)
					throws DatasetTransportException {
				return null;
			}

			@Override
			public ListenableFuture<Result> find(final String opId)
					throws DatasetTransportException, DatasetUsageException {
				return null;
			}

			@Override
			public boolean delete(final String opId) {
				return false;
			}
		};
		return new EngineAdapter(engine, repo, selector);
	}

	private final DatasetEngine delegate;
	private final FormatSelector selector;
	private final ResultRepository results;

	public EngineAdapter(final DatasetEngine delegate,
			final ResultRepository results, final FormatSelector selector) {
		super();
		this.delegate = delegate;
		this.results = results;
		this.selector = selector;
	}

	/**
	 * Attempt to complete the given {@link OperationBuilder partial operation}
	 * with a {@link SerializationFormat} that is supported by the backing
	 * engine and is compatible to the content types accepted by the given
	 * request.
	 * 
	 * @param request
	 *            holding acceptable variants
	 * @param partial
	 *            operation in construction
	 * @return completed operation with the selected format
	 * @throws ActionNotSupportedException
	 *             if the given action is not supported by the backing engine
	 * @throws VariantsNotAcceptableException
	 *             if none of the accepted content types in the request can be
	 *             mapped to a {@link SerializationFormat}
	 */
	public DatasetOperation completeWithMatchingFormat(final Request request,
			final OperationBuilder partial) {
		try {
			final SerializationFormat selected = selector.selectFormat(request,
					partial.getAction());
			return partial.renderAs(selected);
		} catch (final DatasetUsageException e) {
			// set contextual information on error
			e.setFailedOperation(partial.invalidate());
			throw e;
		}
	}

	/**
	 * Prepare result storage and forward the given operation to the enclosed
	 * {@link DatasetEngine}. If an error occurs, wrap it as a
	 * {@link DatasetException} if necessary and return a failed future.
	 * 
	 * @param operation
	 *            to be executed
	 * @return future holding the result or an exception if the submission fails
	 */
	public ListenableFuture<Result> submit(final DatasetOperation operation) {
		try {
			final ResultHandler handler = results.newHandlerFor(operation);
			delegate.submit(operation, handler);
			return handler.asFutureResult();
		} catch (final DatasetException e) {
			e.setFailedOperation(operation);
			return Futures.immediateFailedFuture(e);
		} catch (final Exception e) {
			final DatasetFailureException wrapper = new DatasetFailureException(
					e);
			wrapper.setFailedOperation(operation);
			return Futures.immediateFailedFuture(wrapper);
		}
	}

}
