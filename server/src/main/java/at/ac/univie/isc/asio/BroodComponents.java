package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.engine.DatasetHolder;
import at.ac.univie.isc.asio.tool.Timeout;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Paths;

/**
 * Components required for the container management in brood.
 */
@Configuration
@Brood
class BroodComponents {

  @Autowired
  private AsioSettings config;

  @Bean
  public FileSystemConfigStore fileSystemConfigStore(final Timeout timeout) {
    return new FileSystemConfigStore(Paths.get(config.getHome()), timeout);
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
  public DatasetHolder activeDataset() {
    return new DatasetHolder();
  }

  @Bean
  @Primary
  public ResourceConfig broodJerseyConfiguration(final ResourceConfig jersey) {
    jersey.setApplicationName("jersey-brood");
    jersey.register(ApiResource.class);
    if (config.feature.isVphUriAuth()) {
      jersey.register(UriBasedRoutingResource.class);
    } else {
      jersey.register(DefaultRoutingResource.class);
    }
    return jersey;
  }
}
