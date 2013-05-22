package at.ac.univie.isc.asio.frontend;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.ws.rs.core.Variant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.OperationFactory;
import at.ac.univie.isc.asio.Result;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Endpoint for read-only SQL queries.
 * 
 * @author Chris Borckholder
 */
@Path("/query/")
public final class SqlQueryEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(SqlQueryEndpoint.class);

	private static final String PARAM_QUERY = "query";

	private final DatasetEngine engine;
	private final OperationFactory create;
	private final VariantConverter converter;

	private Map<Variant, SerializationFormat> variant2format;

	public SqlQueryEndpoint(final DatasetEngine engine,
			final OperationFactory create) {
		super();
		this.engine = engine;
		this.create = create;
		converter = VariantConverter.getInstance();
		initializeVariants();
	}

	/**
	 * Create the reverse mapping between variants and serialization formats.
	 */
	@VisibleForTesting
	void initializeVariants() {
		final Set<SerializationFormat> formats = engine.supportedFormats();
		final Builder<Variant, SerializationFormat> map = ImmutableMap
				.builder();
		for (final SerializationFormat each : formats) {
			final Variant variant = converter.asVariant(each.asMediaType());
			map.put(variant, each);
		}
		variant2format = map.build();
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

	private SerializationFormat matchFormat(final Request request) {
		final List<Variant> candidates = ImmutableList.copyOf(variant2format
				.keySet()); // not really copying
		final Variant selected = request.selectVariant(candidates);
		if (selected != null) {
			return variant2format.get(selected);
		} else {
			throw new WebApplicationException(Response
					.notAcceptable(candidates).build());
		}
	}
}
