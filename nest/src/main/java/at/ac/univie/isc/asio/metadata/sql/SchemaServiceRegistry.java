package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.container.CatalogEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Lookup the relational structure of a schema with given name. Internal mappings are updated when
 * schemas are deployed and dropped.
 */
@Service
final class SchemaServiceRegistry implements RelationalSchemaService {
  private static final Logger log = getLogger(SchemaServiceRegistry.class);

  private final ConcurrentMap<Schema, RelationalSchemaService> registry = new ConcurrentHashMap<>();

  public SchemaServiceRegistry() {
    log.info(Scope.SYSTEM.marker(), "relational schema service registry enabled");
  }

  @Override
  public SqlSchema explore(final Schema target) throws Schema.NotFound {
    final RelationalSchemaService provider = findDeployedService(target);
    return provider.explore(target);
  }

  private RelationalSchemaService findDeployedService(final Schema target) {
    final RelationalSchemaService schemaService = registry.get(target);
    if (schemaService == null) {
      throw new Schema.NotFound(target);
    }
    return schemaService;
  }

  @Subscribe
  public void onDeploy(final CatalogEvent.SchemaDeployed event) {
    log.debug(Scope.SYSTEM.marker(), "registering schema service for <{}>", event.getName());
    final RelationalSchemaService schemaService = event.getContainer().schemaService();
    registry.put(event.getName(), schemaService);
  }

  @Subscribe
  public void onDrop(final CatalogEvent.SchemaDropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing schema service for <{}>", event.getName());
    registry.remove(event.getName());
  }
}
