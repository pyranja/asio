package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.jena.JenaEngine;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("federation")
public class FederationNodeConfiguration {
  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(FederationNodeConfiguration.class);

  @Autowired
  private TimeoutSpec globalTimeout;

  @Bean
  public JenaEngine jenaFederationConnector() {
    log.info("[BOOT] creating jena federation engine");
    return new JenaEngine(emptyModel(), globalTimeout);
  }

  @Bean(destroyMethod = "close")
  public Model emptyModel() {
    return ModelFactory.createDefaultModel();
  }
}
