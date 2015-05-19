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
