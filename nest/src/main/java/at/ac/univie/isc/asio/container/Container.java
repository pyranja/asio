package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;

import java.util.Set;

/**
 * Facade to a single data set.
 */
public interface Container {
  /**
   * Local name of this schema. Equal to the mysql schema name.
   *
   * @return the local name of this schema
   */
  Schema name();

  /**
   * Global name of this schema, e.g. as used in a metadata repository.
   *
   * @return the global identifier of this schema
   */
  String identifier();

  /**
   * All configured engines for this schema, i.e. sql and sparql.
   *
   * @return set of sql and sparql engine
   */
  Set<Engine> engines();

  /**
   * {@code RelationalSchemaService} of this container.
   *
   * @return a service providing the relational structure of the backing database
   */
  RelationalSchemaService schemaService();
}
