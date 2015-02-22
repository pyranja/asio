package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Scope;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Central registry to look up and manage deployed {@link SpringSchemaContainer}.
 * Adding or removing a schema will trigger {@code SchemaDeployed} and {@code SchemaDropped} events
 * respectively.
 */
@Service
public final class Catalog implements AutoCloseable {
  private static final Logger log = getLogger(Catalog.class);

  private final EventBus events;
  private final ConcurrentMap<String, Schema> catalog;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  @Autowired
  public Catalog(final EventBus events) {
    catalog = new ConcurrentHashMap<>();
    this.events = events;
  }

  /**
   * Find the schema with given name if it exists.
   *
   * @param name name of required schema
   * @return the schema if present
   */
  public Optional<Schema> findByName(final String name) {
    ensureOpen();
    return Optional.fromNullable(catalog.get(name));
  }

  /**
   * Find all schemas currently deployed.
   *
   * @return snapshot of all present schemas
   */
  public Set<Schema> findAll() {
    ensureOpen();
    return ImmutableSet.copyOf(catalog.values());
  }

  /**
   * Add the given schema to this catalog, replacing any schema with the same name.
   *
   * @param schema schema that will be deployed
   * @return the replaced schema if one existed
   */
  public Optional<Schema> deploy(final Schema schema) {
    log.debug(Scope.SYSTEM.marker(), "deploy {}", schema);
    ensureOpen();
    requireNonNull(schema);
    final Optional<Schema> former = Optional.fromNullable(catalog.put(schema.name(), schema));
    if (former.isPresent()) {
      events.post(new SchemaDropped(former.get()));
    }
    events.post(new SchemaDeployed(schema));
    return former;
  }

  /**
   * Remove the schema with the given name from this catalog if one is present.
   *
   * @param name name of the schema that will be dropped
   * @return the removed schema if one existed
   */
  public Optional<Schema> drop(final String name) {
    log.debug(Scope.SYSTEM.marker(), "drop {}", name);
    ensureOpen();
    final Optional<Schema> removed = Optional.fromNullable(catalog.remove(name));
    if (removed.isPresent()) {
      events.post(new SchemaDropped(removed.get()));
    }
    return removed;
  }

  private void ensureOpen() {
    if (closed.get()) {
      throw new IllegalStateException("catalog already closed");
    }
  }

  /**
   * Drop all schemas.
   */
  @Override
  public void close() {
    closed.set(true);
    final ImmutableSet<Schema> remaining = ImmutableSet.copyOf(catalog.values());
    catalog.clear();
    for (Schema schema : remaining) {
      events.post(new SchemaDropped(schema));
    }
  }

  // events

  /** raised whenever a schema is added to the catalog */
  public static class SchemaDeployed extends Event {
    private SchemaDeployed(final Schema schema) {
      super(schema);
    }
  }
  /** raised whenever a schema is removed from the catalog */
  public static class SchemaDropped extends Event {
    private SchemaDropped(final Schema schema) {
      super(schema);
    }
  }

  static abstract class Event {
    public final Schema schema;

    protected Event(final Schema schema) {
      this.schema = schema;
    }
  }
}
