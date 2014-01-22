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
import at.ac.univie.isc.asio.common.IdGenerator;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.DatasetExceptionMapper;
import at.ac.univie.isc.asio.frontend.LogContextFilter;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.VariantConverter;
import at.ac.univie.isc.asio.protocol.EndpointSupplier;
import at.ac.univie.isc.asio.protocol.EntryPoint;
import at.ac.univie.isc.asio.protocol.OperationParser;
import at.ac.univie.isc.asio.protocol.PrototypeEngineProvider;
import at.ac.univie.isc.asio.transport.JdkPipeTransferFactory;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.base.Supplier;
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

  @Bean(name = "asio_entry")
  public EntryPoint entryPoint() {
    return new EntryPoint(endpointSupplier());
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
  public EndpointSupplier endpointSupplier() {
    log.info("[BOOT] using engines {}", engines);
    return new PrototypeEngineProvider(engines, parser(), processor(), transferFactory());
  }

  @Bean
  public OperationParser parser() {
    final IdGenerator ids = RandomIdGenerator.withPrefix("asio");
    final OperationFactory factory = new OperationFactory(ids);
    return new OperationParser(factory);
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

  @Bean
  public Supplier<Transfer> transferFactory() {
    return new JdkPipeTransferFactory();
  }
}
