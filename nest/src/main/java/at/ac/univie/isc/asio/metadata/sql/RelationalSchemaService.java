package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.Schema;
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
   * @throws at.ac.univie.isc.asio.Schema.NotFound if the requested schema is not accessible
   */
  SqlSchema explore(final Schema target) throws Schema.NotFound;
}
