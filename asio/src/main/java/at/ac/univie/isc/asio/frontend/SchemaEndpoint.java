package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

/**
 * Endpoint that serves the schema of a Dataset, e.g. the relational schema.
 * 
 * @author Chris Borckholder
 */
@Path("/schema/")
public final class SchemaEndpoint extends AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(SchemaEndpoint.class);

	SchemaEndpoint(final FrontendEngineAdapter engine,
			final AsyncProcessor processor, final OperationFactory create) {
		super(engine, processor, create, Action.SCHEMA);
	}

	/**
	 * Delivers the schema of this DatasetEngine.
	 * 
	 * @return schema of this dataset
	 */
	@GET
	public void serveSchema(@Context final Request request,
			@Suspended final AsyncResponse response) {
		log.info("serving schema");
		final OperationBuilder partial = create.schema();
		complete(partial, request, response);
	}
}
