package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.SqlSchema;
import rx.Observable;

/**
 * Define a service to inspect structural information on a dataset in the backing database.
 */
public interface DefinitionService {
  /**
   * Inspect the backing database schema of a dataset. Results include structural information on
   * relational tables, columns and relationships. The {@code Observable} may either return a single
   * definition on success or an error if the inspection failed, e.g. the target schema does not
   * exist.
   *
   * @param name name of the schema in the database
   * @return reactive sequence of inspection results
   */
  Observable<SqlSchema> definition(final String name);
}
