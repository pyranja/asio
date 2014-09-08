package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
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

import static com.google.common.base.Strings.emptyToNull;

@Configuration
@Profile("dataset")
public class AsioJenaConfiguration {
  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(AsioJenaConfiguration.class);

  @Autowired
  private Environment env;
  @Autowired
  private ServletContext webContext;
  @Autowired
  private TimeoutSpec globalTimeout;

  @Bean
  public JenaEngine sparqlEngine() {
    final Model model = d2rModel();
    log.info("[BOOT] using model {}", model);
    final boolean allowFederated =
        env.getProperty("asio.sparql.allowFederated", Boolean.class, Boolean.FALSE);
    return new JenaEngine(model, globalTimeout, allowFederated);
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
    final ModelD2RQ model = d2rLoader().getModelD2RQ();
    model.withDefaultMappings(PrefixMapping.Extended);
    return model;
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
