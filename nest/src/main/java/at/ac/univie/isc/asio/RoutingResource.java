package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.container.ContainerResource;
import at.ac.univie.isc.asio.insight.EventResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forward jersey requests to correct components.
 */
@Component
@Path("/api")
public class RoutingResource {
  private static final Logger log = getLogger(RoutingResource.class);

  private final ContainerResource container;
  private final WhoamiResource whoami;
  private final EventResource events;

  @Autowired
  public RoutingResource(final ContainerResource container,
                         final WhoamiResource whoami,
                         final EventResource events) {
    this.events = events;
    log.info(Scope.SYSTEM.marker(), "active");
    this.container = container;
    this.whoami = whoami;
  }

  @Path("/container")
  public ContainerResource forwardToContainer() {
    return container;
  }

  @Path("/whoami")
  public WhoamiResource forwardToWhoami() {
    return whoami;
  }

  @Path("/events")
  public EventResource forwardToEvents() {
    return events;
  }
}
