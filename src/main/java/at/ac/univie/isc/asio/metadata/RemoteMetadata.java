package at.ac.univie.isc.asio.metadata;

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch metadata from remote repository, serve dummy if not available.
 * Created by pyranja on 29/03/2014.
 */
public class RemoteMetadata implements Supplier<DatasetMetadata> {

  private static final Logger log = LoggerFactory.getLogger(RemoteMetadata.class);

  private final AtosMetadataService proxy;
  private final String id;

  public RemoteMetadata(final AtosMetadataService proxy, final String id) {
    this.proxy = proxy;
    this.id = id;
  }

  @Override
  public DatasetMetadata get() {
    try {
      return proxy.fetchMetadataForId(id);
    } catch (MetadataNotFound | RepositoryFailure e) {
      log.warn("metadata request failed - serving dummy", e);
      return StaticMetadata.NOT_AVAILABLE;
    }
  }
}
