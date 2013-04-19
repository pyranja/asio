package at.ac.univie.isc.vqs;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A protocol endpoint for read-only query operations.
 * 
 * @author Chris Borckholder
 */
@Path("/query/")
@Produces(MediaType.APPLICATION_XML)
public interface QueryEndpoint {

	/**
	 * Accept queries from the request URL's query string
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@GET
	Response acceptUriQuery(@QueryParam("query") final String query);

	/**
	 * Accept queries submitted as url encoded form parameters.
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	Response acceptFormQuery(@FormParam("query") final String query);

	/**
	 * Accept queries submitted as unencoded request entity
	 * 
	 * @param query
	 *            to be executed
	 * @return query results
	 */
	@POST
	@Consumes("application/sql-query")
	Response acceptQuery(final String query);

}