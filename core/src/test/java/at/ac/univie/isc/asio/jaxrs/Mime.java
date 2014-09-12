package at.ac.univie.isc.asio.jaxrs;

import javax.ws.rs.core.MediaType;

/**
 * Common request and response types supported in asio.
 */
public enum Mime {
  // request types
  QUERY_SQL(MediaType.valueOf("application/sql-query"))
  , QUERY_SPARQL(MediaType.valueOf("application/sparql-query"))
  , UPDATE_SQL(MediaType.valueOf("application/sql-update"))
  , UPDATE_SPARQL(MediaType.valueOf("application/sparql-update"))
  // response types
  , CSV(MediaType.valueOf("text/csv"))
  , XML(MediaType.APPLICATION_XML_TYPE)
  , JSON(MediaType.APPLICATION_JSON_TYPE)
  ;

  private final MediaType type;

  private Mime(final MediaType type) {
    this.type = type;
  }

  public MediaType type() {
    return type;
  }
}
