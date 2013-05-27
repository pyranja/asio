package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Generic request processing for DatasetEndpoints.
 * 
 * @author Chris Borckholder
 */
public class AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(AbstractEndpoint.class);

	// dependencies
	private final FrontendEngineAdapter engine;
	private final AsyncProcessor processor;
	protected final OperationFactory create;
	// utils
	private final Action type;

	/**
	 * subclass constructor
	 * 
	 * @param engine
	 *            adapted backing dataset
	 * @param processor
	 *            completes requests asynchronously
	 * @param create
	 *            operation factory
	 * @param type
	 *            of concrete endpoint
	 */
	protected AbstractEndpoint(final FrontendEngineAdapter engine,
			final AsyncProcessor processor, final OperationFactory create,
			final Action type) {
		super();
		this.engine = engine;
		this.processor = processor;
		this.create = create;
		this.type = type;
	}

	/**
	 * Complete the operation information and execute it.
	 * 
	 * @param partial
	 *            dataset operation
	 * @param request
	 *            JAXRS request
	 * @param response
	 *            JAXRS suspenden response
	 */
	protected final void complete(final OperationBuilder partial,
			final Request request, final AsyncResponse response) {
		final SerializationFormat selected = engine.selectFormat(request, type);
		log.debug("selected format {}", selected);
		final DatasetOperation operation = partial.renderAs(selected);
		// TODO set operation in MDC
		log.info("submitting operation {}", operation);
		final ListenableFuture<Result> future = engine.submit(operation);
		log.info("operation submitted {}", operation);
		processor.handle(future, response);
	}
}