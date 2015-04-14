package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.Engine;

import java.util.Set;

/**
 * Facade to a single data set.
 */
public interface Container extends AutoCloseable, Dataset {

  /**
   * All configured engines for this schema, i.e. sql and sparql.
   *
   * @return set of sql and sparql engine
   */
  Set<Engine> engines();

  // === lifecycle =================================================================================

  /**
   * Allocate required resources and attempt to start all components, which are part of this
   * container. A container may only be activated once.
   *
   * @throws IllegalStateException if activated more than once.
   */
  void activate() throws IllegalStateException;

  /**
   * Release all resources associated to this container.
   */
  @Override
  void close();
}
