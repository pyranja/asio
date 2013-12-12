package at.ac.univie.isc.asio.config;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.coordination.EngineSpec.Type;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.DatasetExceptionMapper;
import at.ac.univie.isc.asio.frontend.EngineSelector;
import at.ac.univie.isc.asio.frontend.LogContextFilter;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.QueryEndpoint;
import at.ac.univie.isc.asio.frontend.SchemaEndpoint;
import at.ac.univie.isc.asio.frontend.UpdateEndpoint;
import at.ac.univie.isc.asio.frontend.VariantConverter;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Setup the asio endpoint infrastructure.
 * 
 * @author Chris Borckholder
 */
@Configuration
@ImportResource(value = {"classpath:/spring/asio-cxf.xml"})
@PropertySource(value = {"classpath:/asio.properties"})
public class AsioConfiguration {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(AsioConfiguration.class);

  // asio backend components

  @Autowired
  private Set<DatasetEngine> engines;

  // JAX-RS service endpoints

  @Bean(name = "asio_query")
  public QueryEndpoint queryService() {
    return new QueryEndpoint(registry(), processor(), operationFactory(), Type.SQL);
  }

  @Bean(name = "asio_schema")
  public SchemaEndpoint schemaService() {
    return new SchemaEndpoint(registry(), processor(), operationFactory(), Type.SQL);
  }

  @Bean(name = "asio_update")
  public UpdateEndpoint updateService() {
    return new UpdateEndpoint(registry(), processor(), operationFactory(), Type.SQL);
  }

  @Bean(name = "sparql_query")
  public QueryEndpoint sparqlQuery() {
    return new QueryEndpoint(registry(), processor(), operationFactory(), Type.SPARQL);
  }

  // JAX-RS provider

  @Bean(name = "asio_error_mapper")
  public DatasetExceptionMapper errorMapper() {
    return new DatasetExceptionMapper();
  }

  @Bean(name = "asio_log_filter")
  public LogContextFilter logFilter() {
    return new LogContextFilter();
  }

  // asio frontend components

  @Bean
  public OperationFactory operationFactory() {
    return new OperationFactory(RandomIdGenerator.withPrefix("asio"));
  }

  // @Bean
  // public EngineAdapter engineAdapter() {
  // final FormatSelector selector = new FormatSelector(engines.supports(), converter());
  // return new EngineAdapter(engines, selector);
  // }

  @Bean
  public EngineSelector registry() {
    log.info("[BOOT] using engines {}", engines);
    return new EngineSelector(engines);
  }

  @Bean
  public AsyncProcessor processor() {
    return new AsyncProcessor(responseExecutor(), converter());
  }

  @Bean(destroyMethod = "shutdown")
  public ExecutorService responseExecutor() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("response-processer-%d").build();
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5, factory));
  }

  @Bean
  public VariantConverter converter() {
    return new VariantConverter();
  }
}
