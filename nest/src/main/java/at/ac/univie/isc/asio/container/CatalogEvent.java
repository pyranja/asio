package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;

import javax.annotation.Nonnull;

/**
 * Base for events emitted when modifying the set of active schemas.
 */
public abstract class CatalogEvent {
  private final Container container;

  private CatalogEvent(@Nonnull final Container container) {
    this.container = container;
  }

  public final Schema getName() {
    return container.name();
  }

  public final Container getContainer() {
    return container;
  }

  // === concrete event types ======================================================================

  /**
   * raised whenever a schema is added to the catalog
   */
  public static final class SchemaDeployed extends CatalogEvent {
    public SchemaDeployed(final Container container) {
      super(container);
    }
  }

  /**
   * raised whenever a schema is removed from the catalog
   */
  public static final class SchemaDropped extends CatalogEvent {
    public SchemaDropped(final Container container) {
      super(container);
    }
  }
}
