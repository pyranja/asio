package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.engine.d2rq.D2rqLoader;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
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
    log.info(AsioConfiguration.SYSTEM, "using model {}", model);
    final boolean allowFederated =
        env.getProperty("asio.sparql.allowFederated", Boolean.class, Boolean.FALSE);
    return new JenaEngine(model, globalTimeout, allowFederated);
  }

  @Bean
  public DatasourceSpec datasource() {
    final DatasourceSpec spec = d2rLoader().datasourceSpec();
    if (spec.getPassword() == null) {
      AsioJenaConfiguration.log.warn(AsioConfiguration.SYSTEM, "no password set for JDBC connection {}", spec.getJdbcUrl());
    }
    return spec;
  }

  @Bean(destroyMethod = "close")
  public Model d2rModel() {
    final Model model = d2rLoader().createModel();
    model.withDefaultMappings(PrefixMapping.Extended);
    return model;
  }

  @Bean
  public D2rqLoader d2rLoader() {
    final String mapping = env.getProperty("asio.d2r.mapping", "config.ttl");
    return new D2rqLoader(resolve(mapping));
  }

  @Bean
  @Qualifier("asio.meta.id")
  @Primary
  public Supplier<String> mappingDatasetIdResolver() {
    final String maybeId = d2rLoader().baseUri();
    if (emptyToNull(maybeId) == null) {
      log.warn(AsioConfiguration.SYSTEM, "no valid baseURI defined in d2r mapping");
      return Suppliers.ofInstance("unknown");
    } else {
      log.info(AsioConfiguration.SYSTEM, "using d2r baseURI <{}> as dataset id", maybeId);
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
    return D2rqLoader.toAbsolutePath(mappingLocation);

  }
}
