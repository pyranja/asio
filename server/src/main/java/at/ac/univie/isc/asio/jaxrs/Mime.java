/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
