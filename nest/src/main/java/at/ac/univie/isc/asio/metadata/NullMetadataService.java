package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Schema;
import org.springframework.stereotype.Service;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

/**
 * no-op implementation, always returns a generic, fixed descriptor
 */
@Service
public final class NullMetadataService implements MetadataService {
  public static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  static final SchemaDescriptor NULL_METADATA = SchemaDescriptor.empty("none")
      .withActive(false)
      .withLabel("unknown")
      .withDescription("no description")
      .withAuthor("anonymous")
      .withCreated(LOAD_DATE)
      .withUpdated(LOAD_DATE)
      .build();

  @Override
  public SchemaDescriptor describe(final Schema identifier) {
    return NULL_METADATA;
  }
}
