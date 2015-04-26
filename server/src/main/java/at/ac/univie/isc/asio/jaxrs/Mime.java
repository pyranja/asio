package at.ac.univie.isc.asio.jaxrs;

import javax.ws.rs.core.MediaType;

/**
 * Common request and response types supported in asio.
 */
public enum Mime {

  // === operation request types
  QUERY_SQL(MediaType.valueOf("application/sql-query"))
  , QUERY_SPARQL(MediaType.valueOf("application/sparql-query"))
  , UPDATE_SQL(MediaType.valueOf("application/sql-update"))
  , UPDATE_SPARQL(MediaType.valueOf("application/sparql-update"))

  // === specialized operation response types
  , RESULTS_SPARQL_XML(MediaType.valueOf("application/sparql-results+xml"))
  , RESULTS_SPARQL_JSON(MediaType.valueOf("application/sparql-results+json"))

  , GRAPH_XML(MediaType.valueOf("application/rdf+xml"))
  , GRAPH_JSON(MediaType.valueOf("application/rdf+json"))
  , GRAPH_TURTLE(MediaType.valueOf("text/turtle"))

  , RESULTS_SQL_XML(MediaType.valueOf("application/sql-results+xml"))
  , RESULTS_SQL_WEBROWSET(MediaType.valueOf("application/webrowset+xml"))

  // === other
  , VND_ERROR(MediaType.valueOf("application/vnd.error+json"))
  , EVENT_STREAM(MediaType.valueOf("text/event-stream"))
  ;

  private final MediaType type;

  Mime(final MediaType type) {
    this.type = type;
  }

  public MediaType type() {
    return type;
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
