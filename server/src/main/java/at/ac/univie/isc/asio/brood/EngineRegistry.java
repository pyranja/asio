package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.EngineRouter;

import java.util.Set;

/**
 * An {@code EngineRouter}, that uses an internal mapping of {@code Schema} to {@code Container}
 * pairs to find the right {@code Engine}. The mapping may be modified by raising appropriate
 * {@link ContainerEvent events}.
 */
@Brood
final class EngineRegistry extends BaseContainerRegistry implements EngineRouter {
  public EngineRegistry() {
    log.info(Scope.SYSTEM.marker(), "engine registry enabled");
  }

  @Override
  public Engine select(final Command command) throws Language.NotSupported {
    final Container container = find(command.schema());
    return match(command.language(), container.engines());
  }

  private Engine match(final Language language, final Set<Engine> candidates) {
    for (Engine engine : candidates) {
      if (engine.language().equals(language)) {
        return engine;
      }
    }
    throw new Language.NotSupported(language);
  }
}