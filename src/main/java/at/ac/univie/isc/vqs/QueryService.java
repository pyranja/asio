package at.ac.univie.isc.vqs;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/query/")
public class QueryService {

	@GET
	public Response queryGet(@QueryParam("query") final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response queryForm(@FormParam("query") final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	@POST
	@Consumes("application/sql-query")
	public Response queryPost(final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	private String process(final String query) {
		return "Processed query : [" + query + "]";
	}
}
