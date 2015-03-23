package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import rx.Observable;

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
   * Active settings of this schema.
   *
   * @return the settings
   */
  ContainerSettings settings();

  /**
   * All configured engines for this schema, i.e. sql and sparql.
   *
   * @return set of sql and sparql engine
   */
  Set<Engine> engines();

  /**
   * An {@code Observable}, that may emit a single descriptor of this container on subscription.
   * There may not be metadata available, meaning that the observable will be {@code empty}.
   *
   * @return single or zero element sequence of descriptors
   */
  Observable<SchemaDescriptor> metadata();

  /**
   * The relational table structure of this schema, if it is backed by a relational database.
   *
   * @return single or zero element sequence of sql definition
   */
  Observable<SqlSchema> definition();
}
