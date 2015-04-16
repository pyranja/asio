package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.insight.Emitter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Central registry to look up and manage deployed {@link Container}.
 * Adding or removing a container will trigger {@code Deployed} and {@code Dropped} events
 * respectively.
 */
@Brood
/* final */ class Catalog {
  private static final Logger log = getLogger(Catalog.class);

  private final Map<Id, Container> catalog;
  private final Emitter events;

  @Autowired
  public Catalog(final Emitter events) {
    catalog = new HashMap<>();
    this.events = events;
  }

  // === public api ================================================================================

  /**
   * Add the given container to this catalog, replacing any schema with the same name.
   *
   * @param container schema that will be deployed
   * @return the replaced schema if one existed
   */
  public Optional<Container> deploy(final Container container) {
    log.debug(Scope.SYSTEM.marker(), "deploy {} as <{}>", container, container.name());
    requireNonNull(container);
    final Optional<Container> former =
        Optional.fromNullable(catalog.put(container.name(), container));
    if (former.isPresent()) { events.emit(dropEvent(former.get())); }
    events.emit(deployEvent(container));
    return former;
  }

  /**
   * Remove the schema with the given name from this catalog if one is present.
   *
   * @param id name of the schema that will be dropped
   * @return the removed schema if one existed
   */
  public Optional<Container> drop(final Id id) {
    log.debug(Scope.SYSTEM.marker(), "drop <{}>", id);
    final Optional<Container> removed = Optional.fromNullable(catalog.remove(id));
    if (removed.isPresent()) { events.emit(dropEvent(removed.get())); }
    return removed;
  }

  /**
   * {@link #drop(Id) Drop} all present containers.
   *
   * @return all container that were present when closing
   */
  Set<Container> clear() {
    log.info(Scope.SYSTEM.marker(), "clear - dropping all of {}", catalog.keySet());
    final Set<Container> dropped = new HashSet<>();
    final Iterator<Container> present = catalog.values().iterator();
    while (present.hasNext()) {
      final Container next = present.next();
      events.emit(dropEvent(next));
      dropped.add(next);
      present.remove();
    }
    return dropped;
  }

  // === helper ====================================================================================

  private ContainerEvent.Deployed deployEvent(final Container schema) {
    return new ContainerEvent.Deployed(schema);
  }

  private ContainerEvent.Dropped dropEvent(final Container schema) {
    return new ContainerEvent.Dropped(schema);
  }

  @VisibleForTesting
  Map<Id, Container> internal() {
    return catalog;
  }
}
