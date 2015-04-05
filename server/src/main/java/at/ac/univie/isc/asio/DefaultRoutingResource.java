package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.metadata.BaseContainerRegistry;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Route requests to the correct dataset resource.
 */
@Brood
@Path("/{target}")
public class DefaultRoutingResource extends BaseContainerRegistry {
  private final DatasetResource dataset;
  private final WhoamiResource whoami;

  private final Holder<Dataset> activeDataset;

  @Autowired
  public DefaultRoutingResource(final DatasetResource dataset,
                                final WhoamiResource whoami,
                                final Holder<Dataset> activeDataset) {
    this.dataset = dataset;
    this.whoami = whoami;
    this.activeDataset = activeDataset;
  }

  @PostConstruct
  void report() {
    log.info(Scope.SYSTEM.marker(), "default router loaded");
  }

  @Path("/")
  public DatasetResource forwardToDataset(@PathParam("target") final Id target) {
    activeDataset.set(find(target));
    return dataset;
  }

  @Path("/whoami")
  public WhoamiResource forwardToDatasetWhoami() {
    return whoami;
  }
}
