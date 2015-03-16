package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;

/**
 * Adapt some non-standard configuration to the
 * {@link at.ac.univie.isc.asio.container.ContainerSettings internal format}.
 */
public interface ContainerAdapter {
  /**
   * Create {@code ContainerSettings} from the wrapped external configuration for a new schema
   * with the given local name. May use the given {@code ConfigStore} to persist additional
   * configuration items.
   *
   * @param schema local name of the schema, which is configured
   * @param store persistent storage for additional configuration items
   * @return adapted settings
   */
  ContainerSettings translate(Schema schema, ConfigStore store);
}
