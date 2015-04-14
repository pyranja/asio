package at.ac.univie.isc.asio.metadata;

import rx.Observable;

import java.net.URI;

/**
 * Define a service to retrieve metadata of datasets from some (external) source. Metadata lookup
 * requires a global identifier of the requesting dataset.
 */
public interface DescriptorService {
  /**
   * Request metadata for the dataset with the given {@code identifier}. The {@code Observable} may
   * either return a single descriptor on success, nothing at all if no metadata is found, or raise
   * an error if retrieval fails.
   *
   * @param identifier global identifier of the requesting dataset
   * @return reactive sequence of lookup results
   */
  Observable<SchemaDescriptor> metadata(final URI identifier);
}
