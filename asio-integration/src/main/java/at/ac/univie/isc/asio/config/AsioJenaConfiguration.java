package at.ac.univie.isc.asio.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.jena.JenaEngine;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;

import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.server.ConfigLoader;

@Configuration
public class AsioJenaConfiguration {

  @Autowired
  Environment env;
  @Autowired
  ServletContext webContext;

  @Bean
  public DatasetEngine jenaEngine() {
    return new JenaEngine(queryWorkerPool(), d2rModel());
  }

  @Bean(destroyMethod = "close")
  public Model d2rModel() {
    final String mapping = env.getProperty("asio.d2r.mapping", "config.ttl");
    final SystemLoader loader = new SystemLoader();
    loader.setMappingURL(resolve(mapping));
    return loader.getModelD2RQ();
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
      mappingLocation = webContext.getRealPath("WEB-INF/" + mappingLocation);
    }
    return ConfigLoader.toAbsoluteURI(mappingLocation);
  }
}
