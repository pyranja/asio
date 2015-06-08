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
package at.ac.univie.isc.asio.d2rq;

import com.google.common.base.Optional;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * Utility methods for working with d2rq.
 */
public final class D2rqTools {
  /**
   * Default base to resolve relative IRIs
   */
  public static final String DEFAULT_BASE = "asio:///default/";

  private D2rqTools() { /* no instances */ }

  /**
   * Find a single resource with given rdf:type in the model if one is present.
   *
   * @param model model to search in
   * @param type  type of required resource
   * @return the resource if present
   * @throws IllegalArgumentException if multiple resources with matching type are found
   */
  static Optional<Resource> findSingleOfType(final Model model, final Resource type) {
    final ResIterator it = model.listResourcesWithProperty(RDF.type, type);
    final Optional<Resource> found = it.hasNext()
        ? Optional.of(it.nextResource())
        : Optional.<Resource>absent();
    if (found.isPresent() && it.hasNext()) {
      throw new IllegalArgumentException("found multiple <" + type + "> resources");
    }
    return found;
  }

  /**
   * Find the embedded base resource IRI if present.
   *
   * @param model given configuration
   * @return base resource IRI embedded in model.
   */
  static Optional<String> findEmbeddedBaseUri(final Model model) {
    final Optional<Resource> server = findSingleOfType(model, D2RConfig.Server);
    if (server.isPresent()) {
      final Resource baseUriProperty = server.get().getPropertyResourceValue(D2RConfig.baseURI);
      return baseUriProperty != null
          ? Optional.fromNullable(baseUriProperty.getURI())
          : Optional.<String>absent();
    } else {
      return Optional.absent();
    }
  }

  /**
   * Create a sql connection for d2r from the given settings.
   *
   * @param url jdbc url
   * @param username username
   * @param password password
   * @param properties additional jdbc settings
   * @return a d2r sql connection
   */
  public static ConnectedDB createSqlConnection(final String url,
                                                final String username,
                                                final String password,
                                                final Properties properties) {
    return new ConnectedDB(url, username, password,
        Collections.<String, DataType.GenericType>emptyMap(),
        Database.NO_LIMIT, Database.NO_FETCH_SIZE,
        properties);
  }

  /**
   * Create a d2rq-jena model from the given mapping, using the given connection to a database.
   * The supplied connection will override jdbc configuration in the mapping.
   * An {@link PrefixMapping#Extended extended set} of standard prefixes is added to the model.
   * Expects to find a single configured database.
   *
   * @param mapping    d2rq mapping that should be compiled
   * @param connection connection to the backing database
   * @return a jena model connected to the relational database via d2rq.
   */
  public static Model compile(final Mapping mapping, final ConnectedDB connection) {
    final Database database = expectSingle(mapping.databases());
    database.useConnectedDB(connection);
    connection.setLimit(database.getResultSizeLimit());
    connection.setFetchSize(database.getFetchSize());
    mapping.connect();
    final GraphD2RQ graph = new GraphD2RQ(mapping);
    final Model model = ModelFactory.createModelForGraph(graph);
    model.withDefaultMappings(PrefixMapping.Extended);
    return model;
  }

  /**
   * Attempt to extract a single {@link ConnectedDB jdbc connection} from a d2rq-jena model. The
   * model <strong>MUST</strong> be a {@link de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ d2rq-jena model}
   * and it <strong>MUST</strong> be configured to connect to exactly one backing database.
   * If more than one database connection is found, an exception is thrown. In case of non-d2rq
   * models, behaviour is undefined.
   *
   * @param model a d2rq-jena model
   * @return the single database connection backing the given model, never <code>null</code>
   */
  public static ConnectedDB unwrapDatabaseConnection(final Model model) {
    final Graph graph = model.getGraph();
    assert graph instanceof GraphD2RQ : "expected d2rq graph but found " + graph;
    final Database database = expectSingle(((GraphD2RQ) graph).getMapping().databases());
    return database.connectedDB();
  }

  private static Database expectSingle(final Collection<Database> databases) {
    if (databases.size() != 1) {
      throw new InvalidD2rqConfig(D2RQ.Database, "expected a single definition but found " + databases.size());
    }
    return databases.iterator().next();
  }
}
