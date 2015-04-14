package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.container.ContainerEvent;
import at.ac.univie.isc.asio.container.Container;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Skeleton of an event-aware registry service. Internally maps schema names to deployed containers,
 * and keeps the mapping up to date. Container registration and removal is driven by CatalogEvents.
 */
public abstract class BaseContainerRegistry {
  /**
   * non-static to allow subclass loggers to be distinguished by name
   */
  protected final Logger log = getLogger(this.getClass());

  protected final ConcurrentMap<Id, Container> registry = new ConcurrentHashMap<>();

  protected Container find(final Id target) {
    final Container found = registry.get(target);
    if (found == null) {
      throw new Id.NotFound(target);
    }
    return found;
  }

  @Subscribe
  public final void onDeploy(final ContainerEvent.Deployed event) {
    log.debug(Scope.SYSTEM.marker(), "registering container <{}>", event.getName());
    final Container former = registry.put(event.getName(), event.getContainer());
    if (former != null) {
      log.warn(Scope.SYSTEM.marker(), "replaced <{}> with <{}> on deployment", former.name(), event.getName());
    }
  }

  @Subscribe
  public final void onDrop(final ContainerEvent.Dropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing container <{}>", event.getName());
    final Container removed = registry.remove(event.getName());
    if (removed == null) {
      log.warn(Scope.SYSTEM.marker(), "dropped container <{}> was not present", event.getName());
    }
  }
}
