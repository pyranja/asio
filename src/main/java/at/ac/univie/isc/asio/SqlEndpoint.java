package at.ac.univie.isc.asio;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SqlEndpoint implements QueryEndpoint {

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.univie.isc.asio.QueryEndpoint#acceptUriQuery(java.lang.String)
	 */
	@Override
	public Response acceptUriQuery(final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.univie.isc.asio.QueryEndpoint#acceptFormQuery(java.lang.String)
	 */
	@Override
	public Response acceptFormQuery(final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.univie.isc.asio.QueryEndpoint#acceptQuery(java.lang.String)
	 */
	@Override
	public Response acceptQuery(final String query) {
		return Response.ok(process(query), MediaType.TEXT_PLAIN_TYPE).build();
	}

	private String process(final String query) {
		return "Processed query : [" + query + "]";
	}
}
