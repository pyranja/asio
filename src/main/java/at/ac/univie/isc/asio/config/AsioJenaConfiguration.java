package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.jena.JenaEngine;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.server.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.servlet.ServletContext;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Strings.emptyToNull;

@Configuration
@Profile("dataset")
public class AsioJenaConfiguration {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(AsioJenaConfiguration.class);

  @Autowired
  Environment env;
  @Autowired
  ServletContext webContext;
  @Autowired
  TimeoutSpec globalTimeout;

  @Bean
  public DatasetEngine jenaEngine() {
    return new JenaEngine(queryWorkerPool(), d2rModel()).withTimeout(globalTimeout);
  }

  @Bean
  public DatasourceSpec datasource() {
    // XXX will not work if multiple database bindings are defined in d2r mapping .ttl
    final Database d2rDb = Iterables.getOnlyElement(d2rLoader().getMapping().databases());
    if (d2rDb.getPassword() == null) {
      log.warn("[BOOT] no password set for JDBC connection {}", d2rDb.getJDBCDSN());
    }
    return DatasourceSpec
        .connectTo(d2rDb.getJDBCDSN())
        .with(d2rDb.getJDBCDriver())
        .authenticateAs(d2rDb.getUsername(), d2rDb.getPassword());
  }

  @Bean(destroyMethod = "close")
  public Model d2rModel() {
    return d2rLoader().getModelD2RQ();
  }

  @Bean
  public SystemLoader d2rLoader() {
    final String mapping = env.getProperty("asio.d2r.mapping", "config.ttl");
    final SystemLoader loader = new SystemLoader();
    loader.setMappingURL(resolve(mapping));
    return loader;
  }

  @Bean
  @Qualifier("asio.meta.id")
  @Primary
  public Supplier<String> mappingDatasetIdResolver() {
    final String maybeId = d2rLoader().getServerConfig().baseURI();
    if (emptyToNull(maybeId) == null) {
      log.warn("[BOOT] no valid baseURI defined in d2r mapping");
      return Suppliers.ofInstance("unknown");
    } else {
      log.info("[BOOT] using d2r baseURI <{}> as dataset id", maybeId);
      return Suppliers.ofInstance(maybeId);
    }
  }

  @Bean(destroyMethod = "shutdownNow")
  public ListeningExecutorService queryWorkerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("jena-query-worker-%d").build();
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5, factory));
  }

  // transform a mapping file reference into an absolute URI
  // taken from d2rq.server.WebappInitListener
  private String resolve(String mappingLocation) {
    if (!mappingLocation.matches("[a-zA-Z0-9]+:.*")) {
      // relative file names are resolved against the web app internal folder
      mappingLocation = webContext.getRealPath(mappingLocation);
    }
    return ConfigLoader.toAbsoluteURI(mappingLocation);
  }
}
