package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import rx.Observable;

/**
 * Metadata on a deployed dataset.
 */
public interface Dataset {
  /**
   * Local name of this dataset.
   *
   * @return the local name
   */
  Id name();

  /**
   * An {@code Observable}, that may emit a single descriptor of this dataset on subscription.
   * There may not be metadata available, meaning that the observable will be {@code empty}.
   *
   * @return single or zero element sequence of descriptors
   */
  Observable<SchemaDescriptor> metadata();

  /**
   * The relational table structure of this dataset, if it is backed by a relational database.
   *
   * @return single or zero element sequence of sql definition
   */
  Observable<SqlSchema> definition();
}
