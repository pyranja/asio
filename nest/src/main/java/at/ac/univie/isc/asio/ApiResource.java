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
 * Entry point to the management api resources. Mainly to provide a common prefix.
 */
@Component
@Path("/api")
public class ApiResource {
  private static final Logger log = getLogger(DefaultRoutingResource.class);

  private final ContainerResource container;
  private final WhoamiResource whoami;
  private final EventResource events;

  @Autowired
  public ApiResource(final ContainerResource container, final WhoamiResource whoami, final EventResource events) {
    this.container = container;
    this.whoami = whoami;
    this.events = events;
    log.info(Scope.SYSTEM.marker(), "active");
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
