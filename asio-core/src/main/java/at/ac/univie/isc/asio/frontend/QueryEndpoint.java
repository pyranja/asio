package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

/**
 * Endpoint for read-only SQL queries.
 * 
 * @author Chris Borckholder
 */
@Path("/query/")
public final class QueryEndpoint extends AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(QueryEndpoint.class);

	private static final String PARAM_QUERY = "query";

	public QueryEndpoint(final EngineAdapter engine,
			final AsyncProcessor processor, final OperationFactory create) {
		super(engine, processor, create);
	}

	/**
	 * Accept queries from the request URL's query string.
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@GET
	public void acceptUriQuery(@QueryParam(PARAM_QUERY) final String query,
			@Context final Request request,
			@Suspended final AsyncResponse response) {
		process(query, request, response);
	}

	/**
	 * Accept queries submitted as url encoded form parameters.
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void acceptFormQuery(@FormParam(PARAM_QUERY) final String query,
			@Context final Request request,
			@Suspended final AsyncResponse response) {
		process(query, request, response);
	}

	/**
	 * Accept queries submitted as unencoded request entity
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@POST
	@Consumes("application/sql-query")
	public void acceptQuery(final String query, @Context final Request request,
			@Suspended final AsyncResponse response) {
		process(query, request, response);
	}

	/**
	 * Invoke processing.
	 */
	private void process(final String query, final Request request,
			final AsyncResponse response) {
		log.debug("-- processing \"{}\"", query);
		final OperationBuilder partial = create.query(query);
		complete(partial, request, response);
	}
}
