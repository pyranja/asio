package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;

/**
 * Route {@link Command commands} to matching
 * {@link at.ac.univie.isc.asio.engine.Engine handlers}.
 */
public interface EngineRouter {
  /**
   * Select an appropriate handler for the given command.
   * @param command describes the request
   * @return An {@code Engine} capable of handling the command
   * @throws Language.NotSupported if no matching {@code Engine} is found
   */
  Engine select(Command command) throws Language.NotSupported;
}
