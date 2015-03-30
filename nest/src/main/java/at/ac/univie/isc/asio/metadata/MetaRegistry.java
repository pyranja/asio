package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;
import org.springframework.stereotype.Service;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

/**
 * Lookup metadata on deployed schemas.
 */
@Service
final class MetaRegistry extends BaseContainerRegistry implements MetadataService, RelationalSchemaService {
  private static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  private static final SchemaDescriptor NULL_METADATA = SchemaDescriptor.empty("none")
      .withActive(false)
      .withLabel("unknown")
      .withDescription("no description")
      .withAuthor("anonymous")
      .withCreated(LOAD_DATE)
      .withUpdated(LOAD_DATE)
      .build();

  MetaRegistry() {
    log.info(Scope.SYSTEM.marker(), "meta registry enabled");
    log.debug(Scope.SYSTEM.marker(), "using load date {}", LOAD_DATE);
  }

  @Override
  public SchemaDescriptor describe(final Id target) {
    return find(target).metadata().toBlocking().singleOrDefault(NULL_METADATA);
  }

  @Override
  public SqlSchema explore(final Id target) throws Id.NotFound {
    return find(target).definition().toBlocking().single();
  }
}
