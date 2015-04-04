package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;

/**
 * Add the ignored authority element to the dataset routing path.
 */
@Component
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
