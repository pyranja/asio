package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.container.CatalogEvent;
import at.ac.univie.isc.asio.container.Container;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * An {@code EngineRouter}, that uses an internal mapping of {@code Schema} to {@code Container}
 * pairs to find the right {@code Engine}. The mapping may be modified by raising appropriate
 * {@link CatalogEvent events}.
 */
@Service
final class EngineRegistry implements EngineRouter {
  private static final Logger log = getLogger(EngineRegistry.class);

  private final ConcurrentMap<Schema, Container> containers = new ConcurrentHashMap<>();

  public EngineRegistry() {
    log.info(Scope.SYSTEM.marker(), "engine registry enabled");
  }

  @Override
  public Engine select(final Command command) throws Language.NotSupported {
    final Container container = findContainerFor(command.schema());
    return findMatchingEngine(command.language(), container.engines());
  }

  private Container findContainerFor(final Schema schema) {
    final Container container = containers.get(schema);
    if (container == null) {
      throw new Schema.NotFound(schema);
    }
    return container;
  }

  private Engine findMatchingEngine(final Language language, final Set<Engine> candidates) {
    for (Engine engine : candidates) {
      if (engine.language().equals(language)) {
        return engine;
      }
    }
    throw new Language.NotSupported(language);
  }

  @Subscribe
  public void onDeployed(final CatalogEvent.SchemaDeployed event) {
    log.debug(Scope.SYSTEM.marker(), "adding route for {}", event.getName());
    final Container former = containers.put(event.getName(), event.getContainer());
    if (former != null) {
      log.warn(Scope.SYSTEM.marker(), "replaced {} on deployment", event.getName());
    }
  }

  @Subscribe
  public void onDropped(final CatalogEvent.SchemaDropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing route for {}", event.getName());
    containers.remove(event.getName());
  }
}
