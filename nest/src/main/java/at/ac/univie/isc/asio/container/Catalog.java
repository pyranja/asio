package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Striped;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Central registry to look up and manage deployed {@link SpringContainer}.
 * Adding or removing a container will trigger {@code SchemaDeployed} and {@code SchemaDropped}
 * events respectively.
 */
@Service
final class Catalog<CONTAINER extends Container> {
  private static final Logger log = getLogger(Catalog.class);

  private static final int INITIAL_CAPACITY = 8;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int CONCURRENCY_LEVEL = 4;

  private final ConcurrentMap<Schema, CONTAINER> catalog =
      new ConcurrentHashMap<>(INITIAL_CAPACITY, LOAD_FACTOR, CONCURRENCY_LEVEL);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Striped<Lock> locks = Striped.lock(CONCURRENCY_LEVEL);
  private final EventBus events;

  @Autowired
  public Catalog(final EventBus events) {
    this.events = events;
  }

  /**
   * Find all schemas currently deployed.
   *
   * @return snapshot of all present schemas
   */
  public Set<CONTAINER> findAll() {
    ensureOpen();
    return ImmutableSet.copyOf(catalog.values());
  }

  /**
   * Add the given schema to this catalog, replacing any schema with the same name.
   *
   * @param container schema that will be deployed
   * @return the replaced schema if one existed
   */
  public Optional<CONTAINER> deploy(final CONTAINER container) {
    log.debug(Scope.SYSTEM.marker(), "deploy {} as <{}>", container, container.name());
    ensureOpen();
    requireNonNull(container);
    final Optional<CONTAINER> former = Optional.fromNullable(catalog.put(container.name(), container));
    if (former.isPresent()) { events.post(dropEvent(former.get())); }
    events.post(deployEvent(container));
    return former;
  }

  /**
   * Remove the schema with the given name from this catalog if one is present.
   *
   * @param schema name of the schema that will be dropped
   * @return the removed schema if one existed
   */
  public Optional<CONTAINER> drop(final Schema schema) {
    log.debug(Scope.SYSTEM.marker(), "drop <{}>", schema);
    ensureOpen();
    final Optional<CONTAINER> removed = Optional.fromNullable(catalog.remove(schema));
    if (removed.isPresent()) { events.post(dropEvent(removed.get())); }
    return removed;
  }

  // === controller handles ========================================================================

  /**
   * Attempt to lock the given schema.
   *
   * @param schema  name of schema that should be locked
   * @param timeout maximal time to wait for a lock
   * @throws IllegalStateException if the lock cannot be acquired in the granted time span
   */
  void lock(final Schema schema, final TimeoutSpec timeout) throws IllegalStateException {
    final Lock lock = locks.get(schema);
    final long waitForMs = timeout.getAs(TimeUnit.MILLISECONDS, 0L);
    try {
      if (!lock.tryLock(waitForMs, TimeUnit.MILLISECONDS)) {
        throw new IllegalStateException("timed out while locking " + schema);
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException("interrupted while locking " + schema, e);
    }
  }

  /**
   * Unlock the given schema
   *
   * @param schema name of schema that should be unlocked.
   */
  void unlock(final Schema schema) {
    locks.get(schema).unlock();
  }

  /**
   * Disable this catalog, all existing schemas are dropped and future calls to
   * {@link #deploy(Container)} or {@link #drop(at.ac.univie.isc.asio.Schema)} will fail.
   *
   * @return all schemas that were present when closing
   */
  Set<CONTAINER> clear() {
    closed.set(true);
    final Set<CONTAINER> remaining = ImmutableSet.copyOf(catalog.values());
    log.info(Scope.SYSTEM.marker(), "cleared - dropping all of {}", remaining);
    for (CONTAINER container : remaining) {
      events.post(dropEvent(container));
    }
    return remaining;
  }

  // === helper ====================================================================================

  private CatalogEvent.SchemaDeployed deployEvent(final CONTAINER schema) {
    return new CatalogEvent.SchemaDeployed(schema);
  }

  private CatalogEvent.SchemaDropped dropEvent(final CONTAINER schema) {
    return new CatalogEvent.SchemaDropped(schema);
  }

  private void ensureOpen() {
    if (closed.get()) {
      throw new IllegalStateException("catalog already closed");
    }
  }
}
