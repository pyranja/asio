package at.ac.univie.isc.asio;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import at.ac.univie.isc.asio.transport.FileResult;

public class SqlEndpoint implements QueryEndpoint {

	private final DatasetEngine engine;

	SqlEndpoint(final DatasetEngine engine) {
		super();
		this.engine = engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.univie.isc.asio.QueryEndpoint#acceptUriQuery(java.lang.String)
	 */
	@Override
	public Response acceptUriQuery(final String query) {
		return process(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.univie.isc.asio.QueryEndpoint#acceptFormQuery(java.lang.String)
	 */
	@Override
	public Response acceptFormQuery(final String query) {
		return process(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.univie.isc.asio.QueryEndpoint#acceptQuery(java.lang.String)
	 */
	@Override
	public Response acceptQuery(final String query) {
		return process(query);
	}

	private Response process(final String query) {
		try {
			final FileResult result = engine.submit(query);
			return Response.ok(result.getInput(),
					MediaType.APPLICATION_XML_TYPE).build();
		} catch (final DatasetUsageException e) {
			throw new WebApplicationException(e, Response
					.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		} catch (final Exception e) {
			throw new WebApplicationException(e, Response.serverError()
					.entity(e.getMessage()).build());
		}
	}
}
