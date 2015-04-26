package at.ac.univie.isc.asio.engine.sparql;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;

import java.security.Principal;

/**
 * Provide resource required for sparql query execution.
 */
public interface JenaFactory extends AutoCloseable {
  /**
   * Parse a string containing a sparql query. Reject illegal queries.
   *
   * @param sparql sparql query text
   * @return parsed sparql query
   */
  Query parse(String sparql);

  /**
   * Turn a sparql query into an executable wrapper and initialize it.
   *
   * @param query parsed sparql query
   * @param owner credentials for federated queries
   * @return jena query execution wrapper
   */
  QueryExecution execution(Query query, Principal owner);

  /**
   * Release all resources. May interrupt running queries.
   */
  @Override
  void close();
}
