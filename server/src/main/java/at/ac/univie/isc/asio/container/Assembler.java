package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Id;
import com.google.common.io.ByteSource;

/**
 * A factory for {@link Container}.
 */
public interface Assembler {
  /**
   * Create a container with the given name from raw configuration data.
   *
   * @param name name of container
   * @param source raw configuration data, e.g. a config file
   * @return the assembled, dormant container
   */
  Container assemble(Id name, ByteSource source);
}
