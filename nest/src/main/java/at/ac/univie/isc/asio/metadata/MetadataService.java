package at.ac.univie.isc.asio.metadata;

/**
 * Facade for metadata fetching.
 */
public interface MetadataService {
  /**
   * Gather metadata about this dataset.
   *
   * @return descriptor of this dataset
   */
  DatasetDescription fetchDescriptor();
}
