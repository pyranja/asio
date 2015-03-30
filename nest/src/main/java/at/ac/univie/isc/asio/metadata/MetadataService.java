package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Id;

/**
 * Facade for metadata fetching.
 */
public interface MetadataService {
  /**
   * Gather metadata about a schema.
   *
   * @param identifier id of the target schema
   * @return descriptor of the target schema
   */
  SchemaDescriptor describe(Id identifier);
}
