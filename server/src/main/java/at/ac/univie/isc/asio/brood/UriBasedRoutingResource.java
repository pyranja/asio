package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;

/**
 * Add the ignored authority element to the dataset routing path.
 */
@Brood
@Primary
@ConditionalOnProperty(AsioFeatures.VPH_URI_AUTH)
@Path("/{target}/{authority}")
public class UriBasedRoutingResource extends DefaultRoutingResource {
  @Autowired
  public UriBasedRoutingResource(final DatasetResource dataset,
                                 final WhoamiResource whoami,
                                 final Holder<Dataset> activeDataset) {
    super(dataset, whoami, activeDataset);
  }

  @PostConstruct
  @Override
  void report() {
    log.info(Scope.SYSTEM.marker(), "vph-uri-auth routing activated");
  }
}
