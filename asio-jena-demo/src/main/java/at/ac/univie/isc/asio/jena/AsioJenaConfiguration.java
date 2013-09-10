package at.ac.univie.isc.asio.jena;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import at.ac.univie.isc.asio.DatasetEngine;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;

import de.fuberlin.wiwiss.d2rq.SystemLoader;

@Configuration
public class AsioJenaConfiguration {

  @Autowired
  Environment env;

  @Bean
  public DatasetEngine jenaEngine() {
    return new JenaEngine(queryWorkerPool(), d2rModel());
  }

  @Bean(destroyMethod = "close")
  public Model d2rModel() {
    final String mapping = env.getProperty("asio.d2r.mapping", "file:/test.ttl");
    final SystemLoader loader = new SystemLoader();
    loader.setMappingURL(mapping);
    return loader.getModelD2RQ();
  }

  @Bean(destroyMethod = "shutdownNow")
  public ListeningExecutorService queryWorkerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("jena-query-qorker-%d").build();
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5, factory));
  }
}
