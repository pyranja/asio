package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.engine.LanguageConnector;
import at.ac.univie.isc.asio.jena.JenaConnector;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@Profile("federation")
public class FederationNodeConfiguration {
  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(FederationNodeConfiguration.class);

  @Autowired
  private TimeoutSpec globalTimeout;

  @Bean
  public LanguageConnector jenaFederationConnector() {
    log.info("[BOOT] creating jena federation engine");
    final Scheduler worker = Schedulers.from(queryWorkerPool());
    return new JenaConnector(emptyModel(), worker, globalTimeout);
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
