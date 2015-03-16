package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.container.CatalogEvent;
import com.google.common.eventbus.Subscribe;
import net.atos.AtosDataset;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import rx.functions.Func1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Maintain mappings between local schema names and an external metadata identifier. Use the
 * mapping to fetch metadata for deployed schemas.
 */
@Service
@Primary
@ConditionalOnProperty(AsioFeatures.VPH_METADATA)
final class ExternalMetadataRegistry implements MetadataService {
  private static final Logger log = getLogger(ExternalMetadataRegistry.class);

  private static final Func1<AtosDataset, SchemaDescriptor> CONVERT = new Func1<AtosDataset, SchemaDescriptor>() {
    @Override
    public SchemaDescriptor call(final AtosDataset atosDataset) {
      return DescriptorConversion.from(atosDataset).get();
    }
  };

  private final ConcurrentMap<Schema, String> registry = new ConcurrentHashMap<>();
  private final AtosMetadataRepository repository;

  @Autowired
  public ExternalMetadataRegistry(final AtosMetadataRepository repository) {
    log.info(Scope.SYSTEM.marker(), "external metadata registry - using {} - enabled", repository);
    this.repository = repository;
  }

  @Override
  public SchemaDescriptor describe(final Schema target) {
    final String identifier = identifierOf(target);
    return repository.findByLocalId(identifier).map(CONVERT)
        .toBlocking().singleOrDefault(NullMetadataService.NULL_METADATA);
  }

  private String identifierOf(final Schema target) {
    final String identifier = registry.get(target);
    if (identifier == null) {
      throw new Schema.NotFound(target);
    }
    return identifier;
  }

  @Subscribe
  public void onDeploy(final CatalogEvent.SchemaDeployed event) {
    final String identifier = event.getContainer().identifier();
    log.debug(Scope.SYSTEM.marker(), "mapping <{}> to external id <{}>", event.getName(), identifier);
    registry.put(event.getName(), identifier);
  }

  @Subscribe
  public void onDrop(final CatalogEvent.SchemaDropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing mapping for <{}>", event.getName());
    registry.remove(event.getName());
  }
}
