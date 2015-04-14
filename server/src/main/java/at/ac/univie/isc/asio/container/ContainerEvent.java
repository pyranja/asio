package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.insight.Event;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base for events emitted when modifying the set of active schemas.
 */
public abstract class ContainerEvent extends Event {

  /**
   * Raise whenever a schema is added to the catalog.
   */
  public static final class Deployed extends ContainerEvent {
    public Deployed(final Container container) {
      super("deployed", container);
    }
  }

  /**
   * Raise whenever a schema is removed from the catalog.
   */
  public static final class Dropped extends ContainerEvent {
    public Dropped(final Container container) {
      super("dropped", container);
    }
  }

  // === event implementation ======================================================================

  private final Container container;

  private ContainerEvent(final String subject, final Container container) {
    super("container", subject);
    this.container = container;
  }

  public final Id getName() {
    return container.name();
  }

  @JsonIgnore
  public final Container getContainer() {
    return container;
  }
}
