package at.ac.univie.isc.asio.ogsadai;

import static at.ac.univie.isc.asio.ogsadai.PipeActivities.deliverToStream;
import static at.ac.univie.isc.asio.ogsadai.PipeActivities.sqlQuery;
import static at.ac.univie.isc.asio.ogsadai.PipeActivities.tupleToWebRowSetCharArrays;
import static at.ac.univie.isc.asio.ogsadai.PipeBuilder.pipe;
import static com.google.common.base.Strings.emptyToNull;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.transport.FileResultRepository;

import com.google.common.io.InputSupplier;
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
	private final ResourceID resource;

	OgsadaiEngine(final OgsadaiAdapter ogsadai,
			final FileResultRepository results, final ResourceID resource) {
		super();
		this.ogsadai = ogsadai;
		this.results = results;
		this.resource = resource;
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
	public ListenableFuture<InputSupplier<InputStream>> submit(
			final String query) {
		validateQuery(query);
		final ResultHandler handler = results.newHandler();
		final String handlerId = ogsadai.register(handler);
		log.trace("[{}] registered handler [{}] with exchanger", query,
				handlerId);
		final Workflow workflow = createWorkflow(query, handlerId);
		log.trace("[{}] using workflow :\n{}", query, workflow);
		final CompletionCallback tracker = delegateTo(handler);
		try {
			ogsadai.invoke(workflow, tracker);
		} catch (final DatasetException cause) {
			// clean up exchange
			ogsadai.revokeSupplier(handlerId);
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

	private Workflow createWorkflow(final String query, final String streamId) {
		return pipe(sqlQuery(resource, query)).into(
				tupleToWebRowSetCharArrays()).finish(deliverToStream(streamId));
	}

	private void validateQuery(final String query) {
		if (emptyToNull(query) == null) {
			throw new DatasetUsageException("invalid query \"" + query + "\"");
		}
	}
}
