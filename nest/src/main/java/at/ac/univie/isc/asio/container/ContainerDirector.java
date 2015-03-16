package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Orchestrate creation and removal of schema containers.
 */
@Service
public final class ContainerDirector implements AutoCloseable {
  private static final Logger log = getLogger(ContainerDirector.class);

  private final Catalog<SpringContainer> catalog;
  private final ConfigStore config;
  private final SpringContainerFactory factory;
  private final ObjectMapper jackson;
  private final TimeoutSpec timeout;

  @Autowired
  public ContainerDirector(final Catalog<SpringContainer> catalog,
                           final ConfigStore config,
                           final SpringContainerFactory factory,
                           final ObjectMapper jackson,
                           final TimeoutSpec timeout) {
    this.catalog = catalog;
    this.config = config;
    this.factory = factory;
    this.jackson = jackson;
    this.timeout = timeout;
  }

  /**
   * Create a new schema with given local name from adapted configuration. If a schema with that
   * local name if already present, replace it with the new one.
   *
   * @param schema local name of target schema
   * @param adapter adapted configuration
   * @return local name of the deployed schema
   */
  public Schema createNewOrReplace(final Schema schema, final ContainerAdapter adapter) {
    log.debug(Scope.SYSTEM.marker(), "create or replace <{}>", schema);
    catalog.lock(schema, timeout);
    try {
      dispose(schema);
      final ContainerSettings settings = adapter.translate(schema, config);
      config.save(schema.name(), "settings.json", serialize(settings));
      final SpringContainer container = factory.createFrom(settings);
      log.debug(Scope.SYSTEM.marker(), "created {} for <{}>", container, schema);
      catalog.deploy(container);
    } finally {
      catalog.unlock(schema);
    }
    return schema;
  }

  /**
   * If present undeploy and dispose the schema with given local name.
   *
   * @param schema local name of target schema
   * @return true if the target schema was present and has been dropped, false if no such schema existed
   */
  public boolean dispose(final Schema schema) {
    log.debug(Scope.SYSTEM.marker(), "dispose <{}>", schema);
    catalog.lock(schema, timeout);
    try {
      final Optional<SpringContainer> dropped = catalog.drop(schema);
      if (dropped.isPresent()) {
        final SpringContainer container = dropped.get();
        log.debug(Scope.SYSTEM.marker(), "found {} for <{}> - destroying it", container, schema);
        container.close();
        config.clear(schema.name());
      }
      return dropped.isPresent();
    } finally {
      catalog.unlock(schema);
    }
  }

  @PreDestroy
  @Override
  public void close() {
    log.info(Scope.SYSTEM.marker(), "shutting down");
    final Set<SpringContainer> remaining = catalog.clear();
    for (final SpringContainer container : remaining) {
      try {
        log.debug(Scope.SYSTEM.marker(), "closing {}", container);
        container.close();
      } catch (Exception e) {
        log.error(Scope.SYSTEM.marker(), "failed to close a container", e);
      }
    }
  }

  private ByteSource serialize(final ContainerSettings settings) {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try {
      jackson.writeValue(sink, settings);
    } catch (IOException e) {
      // this should never happen
      throw new IOError(e);
    }
    return ByteSource.wrap(sink.toByteArray());
  }
}
