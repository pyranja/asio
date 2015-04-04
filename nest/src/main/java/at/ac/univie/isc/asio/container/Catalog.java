package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Striped;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Central registry to look up and manage deployed {@link Container}.
 * Adding or removing a container will trigger {@code SchemaDeployed} and {@code SchemaDropped}
 * events respectively.
 */
@Brood
/* final */ class Catalog<CONTAINER extends Container> {
  private static final Logger log = getLogger(Catalog.class);

  private static final int INITIAL_CAPACITY = 8;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int CONCURRENCY_LEVEL = 4;

  private final ConcurrentMap<Id, CONTAINER> catalog =
      new ConcurrentHashMap<>(INITIAL_CAPACITY, LOAD_FACTOR, CONCURRENCY_LEVEL);
  private final Striped<Lock> locks = Striped.lock(CONCURRENCY_LEVEL);
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final EventBus events;
  private final long maximalWaitingTimeInMilliseconds;

  @Autowired
  public Catalog(final EventBus events, final TimeoutSpec timeout) {
    this.events = events;
    // 0 means 'do not wait at all'
    maximalWaitingTimeInMilliseconds = timeout.getAs(TimeUnit.MILLISECONDS, 0L);
  }

  // === public api ================================================================================

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
    final Optional<CONTAINER> former =
        Optional.fromNullable(catalog.put(container.name(), container));
    if (former.isPresent()) { events.post(dropEvent(former.get())); }
    events.post(deployEvent(container));
    return former;
  }

  /**
   * Remove the schema with the given name from this catalog if one is present.
   *
   * @param id name of the schema that will be dropped
   * @return the removed schema if one existed
   */
  public Optional<CONTAINER> drop(final Id id) {
    log.debug(Scope.SYSTEM.marker(), "drop <{}>", id);
    ensureOpen();
    final Optional<CONTAINER> removed = Optional.fromNullable(catalog.remove(id));
    if (removed.isPresent()) { events.post(dropEvent(removed.get())); }
    return removed;
  }

  /**
   * Execute the given callback while holding the lock of given container.
   *
   * @param name     name of container that must be locked during the execution
   * @param action   the callback that should be executed atomically
   * @param <RESULT> type of return value of the callable
   * @return the value returned by the callback
   * @throws RuntimeException            unchecked exceptions are propagated as is from the callback
   * @throws UncheckedExecutionException if the callback throws a checked exception
   * @throws UncheckedTimeoutException   if the lock cannot be acquired in the maximal allowed time
   */
  public <RESULT> RESULT atomic(final Id name, final Callable<RESULT> action)
      throws UncheckedTimeoutException, UncheckedExecutionException {
    lock(name);
    try {
      return action.call();
    } catch (Exception failure) {
      Throwables.propagateIfPossible(failure);
      throw new UncheckedExecutionException(failure);
    } finally {
      unlock(name);
    }
  }

  // === queries ===================================================================================

  /**
   * Find a deployed container with given id.
   *
   * @param id id of container
   * @return deployed container if present
   */
  Optional<CONTAINER> find(final Id id) {
    ensureOpen();
    return Optional.fromNullable(catalog.get(id));
  }

  /**
   * Find the names of all currently deployed containers.
   *
   * @return snapshot of all present container names.
   */
  Set<Id> findKeys() {
    ensureOpen();
    return ImmutableSet.copyOf(catalog.keySet());
  }

  /**
   * Find all schemas currently deployed.
   *
   * @return snapshot of all present schemas
   */
  Set<CONTAINER> findAll() {
    ensureOpen();
    return ImmutableSet.copyOf(catalog.values());
  }

  // === controller handles ========================================================================

  /**
   * Attempt to lock the given schema.
   *
   * @param id name of schema that should be locked
   * @throws UncheckedTimeoutException if the lock cannot be acquired in the granted time span
   */
  void lock(final Id id) throws UncheckedTimeoutException {
    final Lock lock = locks.get(id);
    try {
      if (!lock.tryLock(maximalWaitingTimeInMilliseconds, TimeUnit.MILLISECONDS)) {
        throw new UncheckedTimeoutException("timed out while locking " + id);
      }
    } catch (InterruptedException e) {
      throw new UncheckedTimeoutException("interrupted while locking " + id, e);
    }
  }

  /**
   * Unlock the given schema
   *
   * @param id name of schema that should be unlocked.
   */
  void unlock(final Id id) {
    locks.get(id).unlock();
  }

  /**
   * Disable this catalog, all existing schemas are dropped and future calls to
   * {@link #deploy(Container)} or {@link #drop(Id)} will fail.
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

  /**
   * Whether this catalog has been closed.
   */
  boolean isClosed() {
    return closed.get();
  }

  /**
   * The internal locking mechanism.
   */
  @VisibleForTesting
  Striped<Lock> locks() {
    return locks;
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
