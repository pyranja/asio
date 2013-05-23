package at.ac.univie.isc.asio.frontend;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
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
 * Endpoint that serves the schema of a Dataset, e.g. the relational schema.
 * 
 * @author Chris Borckholder
 */
@Path("/schema/")
public final class SchemaEndpoint extends AbstractEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(SchemaEndpoint.class);

	public SchemaEndpoint(final DatasetEngine engine,
			final OperationFactory create) {
		super(engine, create, Action.SCHEMA);
	}

	/**
	 * Delivers the schema of this DatasetEngine.
	 * 
	 * @return schema of this dataset
	 */
	@GET
	public Response serveSchema(@Context final Request request) {
		log.info("serving schema");
		final SerializationFormat format = matchFormat(request);
		log.debug("selected {}", format);
		DatasetOperation operation = null;
		try {
			operation = create.schema(format);
			final ListenableFuture<Result> future = engine.submit(operation);
			Result result;
			try {
				result = future.get();
			} catch (final ExecutionException e) {
				final Throwable cause = e.getCause();
				log.warn("{} failed with {} as cause", operation, cause);
				if (cause != null && cause instanceof Exception) {
					throw (Exception) cause;
				} else {
					throw e;
				}
			}
			log.info("{} processed successfully", operation);
			final MediaType contentType = converter.asContentType(result
					.mediaType());
			return Response.ok(result.getInput(), contentType).build();
		} catch (final DatasetUsageException e) {
			log.warn("{} failed with user error", operation, e);
			throw new WebApplicationException(e, Response
					.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		} catch (final Exception e) {
			log.warn("{} failed with internal error", operation, e);
			throw new WebApplicationException(e, Response.serverError()
					.entity(e.getMessage()).build());
		}
	}
}
