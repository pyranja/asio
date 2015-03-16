package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.container.ContainerDirector;
import at.ac.univie.isc.asio.container.ContainerSettings;
import at.ac.univie.isc.asio.container.DefaultsAdapter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PublicSchemaLoader implements ApplicationListener<ContextRefreshedEvent> {
  private static final Logger log = getLogger(PublicSchemaLoader.class);

  private final ContainerDirector director;
  private final ContainerSettings settings;
  private volatile boolean loaded = false;

  @Autowired
  public PublicSchemaLoader(final ContainerDirector director, final ContainerSettings settings) {
    this.director = director;
    this.settings = settings;
  }

  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    if (!loaded) {
      loaded = true;
      log.info(Scope.SYSTEM.marker(), "loading public schema");
      director.createNewOrReplace(Schema.valueOf("public"), DefaultsAdapter.from(settings));
    }
  }
}
