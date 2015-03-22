package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.container.CatalogEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import rx.Observable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Maintain mappings between local schema names and an external metadata identifier. Use the
 * mapping to fetch metadata for deployed schemas.
 */
@Service
final class ExternalMetadataRegistry implements MetadataService {
  private static final Logger log = getLogger(ExternalMetadataRegistry.class);

  public static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  static final SchemaDescriptor NULL_METADATA = SchemaDescriptor.empty("none")
      .withActive(false)
      .withLabel("unknown")
      .withDescription("no description")
      .withAuthor("anonymous")
      .withCreated(LOAD_DATE)
      .withUpdated(LOAD_DATE)
      .build();

  private final ConcurrentMap<Schema, Observable<SchemaDescriptor>> registry = new ConcurrentHashMap<>();

  public ExternalMetadataRegistry() {
    log.info(Scope.SYSTEM.marker(), "external metadata registry enabled");
  }

  @Override
  public SchemaDescriptor describe(final Schema target) {
    return delegateFor(target).toBlocking().singleOrDefault(NULL_METADATA);
  }

  private Observable<SchemaDescriptor> delegateFor(final Schema target) {
    final Observable<SchemaDescriptor> identifier = registry.get(target);
    if (identifier == null) {
      throw new Schema.NotFound(target);
    }
    return identifier;
  }

  @Subscribe
  public void onDeploy(final CatalogEvent.SchemaDeployed event) {
    log.debug(Scope.SYSTEM.marker(), "registering metadata delegate for <{}>", event.getName());
    registry.put(event.getName(), event.getContainer().metadata());
  }

  @Subscribe
  public void onDrop(final CatalogEvent.SchemaDropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing metadata delegate for <{}>", event.getName());
    registry.remove(event.getName());
  }
}
