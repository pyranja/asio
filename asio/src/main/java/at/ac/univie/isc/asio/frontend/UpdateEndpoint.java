package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

/**
 * Endpoint that allows the modification of a dataset using UPDATE statements.
 * 
 * @author Chris Borckholder
 */
@Path("/update/")
public class UpdateEndpoint extends AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(UpdateEndpoint.class);

	private static final String PARAM_UPDATE = "update";

	UpdateEndpoint(final FrontendEngineAdapter engine,
			final AsyncProcessor processor, final OperationFactory create) {
		super(engine, processor, create, Action.UPDATE);
	}

	/**
	 * Accept updates submitted as url encoded form parameter.
	 * 
	 * @param update
	 *            to be executed
	 * @return update results
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void acceptFormUpdate(@FormParam(PARAM_UPDATE) final String update,
			@Context final Request request,
			@Suspended final AsyncResponse response) {
		process(update, request, response);
	}

	/**
	 * Accept updates submitted as unencoded request entity
	 * 
	 * @param update
	 *            to be executed
	 * @return update results
	 */
	@POST
	@Consumes("application/sql-update")
	public void acceptUpdate(final String update,
			@Context final Request request,
			@Suspended final AsyncResponse response) {
		process(update, request, response);
	}

	/**
	 * Invoke processing.
	 */
	private void process(final String update, final Request request,
			final AsyncResponse response) {
		log.info("processing \"{}\"", update);
		final OperationBuilder partial = create.update(update);
		complete(partial, request, response);
	}
}
