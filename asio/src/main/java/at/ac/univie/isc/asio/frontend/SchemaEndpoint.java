package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint that serves the schema of a Dataset, e.g. the relational schema.
 * 
 * @author Chris Borckholder
 */
@Path("/schema/")
public final class SchemaEndpoint {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(SchemaEndpoint.class);

	/**
	 * Delivers the schema of this DatasetEngine.
	 * 
	 * @return schema of this dataset
	 */
	@GET
	public Response serveSchema(@Context final Request request) {
		log.info("serving schema");
		// should submit a SCHEMA DatasetOperation
		return Response.serverError().entity("not implemented yet").build();
	}
}
