package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;

/**
 * Collect structural metadata from a RDBMS.
 */
public interface RelationalSchemaService {
  /**
   * Inspect the schema with given name in the backing database.
   *
   * @param target name of the schema
   * @return table structure of the schema
   * @throws Id.NotFound if the requested schema is not accessible
   */
  SqlSchema explore(final Id target) throws Id.NotFound;
}
