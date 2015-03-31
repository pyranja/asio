package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.container.ContainerResource;
import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.insight.EventResource;
import at.ac.univie.isc.asio.metadata.BaseContainerRegistry;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forward jersey requests to correct components.
 */
@Component
@Path("/")
public class RoutingResource extends BaseContainerRegistry {
  private static final Logger log = getLogger(RoutingResource.class);

  private final DatasetResource dataset;
  private final ContainerResource container;
  private final WhoamiResource whoami;
  private final EventResource events;

  private final Holder<Dataset> activeDataset;

  @Autowired
  public RoutingResource(final DatasetResource dataset,
                         final ContainerResource container,
                         final WhoamiResource whoami,
                         final EventResource events,
                         final Holder<Dataset> activeDataset) {
    this.dataset = dataset;
    this.container = container;
    this.whoami = whoami;
    this.events = events;
    this.activeDataset = activeDataset;
    log.info(Scope.SYSTEM.marker(), "active");
  }

  @Path("/api/container")
  public ContainerResource forwardToContainer() {
    return container;
  }

  @Path("/api/whoami")
  public WhoamiResource forwardToWhoami() {
    return whoami;
  }

  @Path("/api/events")
  public EventResource forwardToEvents() {
    return events;
  }

  @Path("/{target}")
  public DatasetResource forwardToDataset(@PathParam("target") final Id target) {
    activeDataset.set(find(target));
    return dataset;
  }
}
