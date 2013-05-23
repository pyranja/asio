package at.ac.univie.isc.asio.frontend;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.OperationFactory;
import at.ac.univie.isc.asio.Result;

import com.google.common.util.concurrent.ListenableFuture;

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

	public QueryEndpoint(final DatasetEngine engine,
			final OperationFactory create) {
		super(engine, create, Action.QUERY);
	}

	/**
	 * Accept queries from the request URL's query string.
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@GET
	public Response acceptUriQuery(@QueryParam(PARAM_QUERY) final String query,
			@Context final Request request) {
		return process(query, request);
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
	public Response acceptFormQuery(@FormParam(PARAM_QUERY) final String query,
			@Context final Request request) {
		return process(query, request);
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
	public Response acceptQuery(final String query,
			@Context final Request request) {
		return process(query, request);
	}

	/**
	 * Delegate query processing to the configured {@link DatasetEngine}.
	 * 
	 * @param query
	 *            received
	 * @return http response containing the query results or an error.
	 */
	// TODO clean up error handling
	private Response process(final String query, final Request request) {
		log.info("processing [{}]", query);
		final SerializationFormat format = matchFormat(request);
		log.debug("selected {}", format);
		try {
			final DatasetOperation operation = create.query(query, format);
			final ListenableFuture<Result> future = engine.submit(operation);
			try {
				final Result result = future.get();
				log.info("processed [{}] successfully", query);
				final MediaType contentType = converter.asContentType(result
						.mediaType());
				return Response.ok(result.getInput(), contentType).build();
			} catch (final ExecutionException e) {
				final Throwable cause = e.getCause();
				log.warn("processing [{}] failed with {} as cause", query,
						cause);
				if (cause != null && cause instanceof Exception) {
					throw (Exception) cause;
				} else {
					throw e;
				}
			}
		} catch (final DatasetUsageException e) {
			log.warn("processing [{}] failed with user error", query, e);
			throw new WebApplicationException(e, Response
					.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		} catch (final Exception e) {
			log.warn("processing [{}] failed with internal error", query, e);
			throw new WebApplicationException(e, Response.serverError()
					.entity(e.getMessage()).build());
		}
	}
}
