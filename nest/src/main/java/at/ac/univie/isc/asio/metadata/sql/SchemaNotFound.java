package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.Pretty;
import at.ac.univie.isc.asio.SchemaIdentifier;

/**
 * Thrown if a requested database schema is not present or not accessible.
 */
public final class SchemaNotFound extends IllegalArgumentException {
  public SchemaNotFound(final SchemaIdentifier name, final String message) {
    super(Pretty.format("schema %s not found (%s)", name, message));
  }
}
