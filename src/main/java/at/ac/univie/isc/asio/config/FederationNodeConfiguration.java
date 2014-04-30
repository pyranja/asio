package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.jena.JenaEngine;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@Profile("federation")
public class FederationNodeConfiguration {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(FederationNodeConfiguration.class);

  @Bean
  public DatasetEngine jenaFederationEngine() {
    log.info("[BOOT] creating jena federation engine");
    return new JenaEngine(queryWorkerPool(), emptyModel());
  }

  @Bean(destroyMethod = "close")
  public Model emptyModel() {
    return ModelFactory.createDefaultModel();
  }

  @Bean(destroyMethod = "shutdownNow")
  public ListeningExecutorService queryWorkerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("jena-query-worker-%d").build();
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5, factory));
  }
}
