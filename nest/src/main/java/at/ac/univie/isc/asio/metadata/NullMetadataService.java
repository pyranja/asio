package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.SchemaIdentifier;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

/**
 * no-op implementation, always returns a generic, fixed descriptor
 */
public final class NullMetadataService implements MetadataService {
  public static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  @Override
  public SchemaDescriptor describe(final SchemaIdentifier identifier) {
    return SchemaDescriptor.empty("none")
        .withActive(false)
        .withLabel("unknown")
        .withDescription("no description")
        .withAuthor("anonymous")
        .withCreated(LOAD_DATE)
        .withUpdated(LOAD_DATE)
        .build();
  }
}
