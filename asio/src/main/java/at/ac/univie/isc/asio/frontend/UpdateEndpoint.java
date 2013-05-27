package at.ac.univie.isc.asio.frontend;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
 * Endpoint that allows the modification of a dataset using UPDATE statements.
 * 
 * @author Chris Borckholder
 */
@Path("/update/")
public class UpdateEndpoint extends AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(UpdateEndpoint.class);

	private static final String PARAM_UPDATE = "update";

	public UpdateEndpoint(final DatasetEngine mockEngine,
			final OperationFactory factory) {
		super(mockEngine, factory, Action.UPDATE);
	}

	/**
	 * Accept updates submitted as url encoded form parameter.
	 * 
	 * @param update
	 *            to be executed
	 * @param request
	 *            jaxrs
	 * @return update results
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response acceptFormUpdate(
			@FormParam(PARAM_UPDATE) final String update,
			@Context final Request request) {
		return process(update, request);
	}

	/**
	 * Accept updates submitted as unencoded request entity
	 * 
	 * @param update
	 *            to be executed
	 * @param request
	 *            jaxrs
	 * @return update results
	 */
	@POST
	@Consumes("application/sql-update")
	public Response acceptUpdate(final String update,
			@Context final Request request) {
		return process(update, request);
	}

	/**
	 * Delegate update processing to the configured {@link DatasetEngine}.
	 * 
	 * @param update
	 *            received
	 * @return http response containing the update results or an error.
	 */
	// TODO clean up error handling
	private Response process(final String update, final Request request) {
		log.info("processing [{}]", update);
		final SerializationFormat format = matchFormat(request);
		log.debug("selected {}", format);
		try {
			final DatasetOperation operation = create.update(update, format);
			final ListenableFuture<Result> future = engine.submit(operation);
			try {
				final Result result = future.get();
				log.info("processed [{}] successfully", update);
				final MediaType contentType = converter.asContentType(result
						.mediaType());
				return Response.ok(result.getInput(), contentType).build();
			} catch (final ExecutionException e) {
				final Throwable cause = e.getCause();
				log.warn("processing [{}] failed with {} as cause", update,
						cause);
				if (cause != null && cause instanceof Exception) {
					throw (Exception) cause;
				} else {
					throw e;
				}
			}
		} catch (final DatasetUsageException e) {
			log.warn("processing [{}] failed with user error", update, e);
			throw new WebApplicationException(e, Response
					.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		} catch (final Exception e) {
			log.warn("processing [{}] failed with internal error", update, e);
			throw new WebApplicationException(e, Response.serverError()
					.entity(e.getMessage()).build());
		}
	}
}
