package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.common.IdGenerator;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.frontend.*;
import at.ac.univie.isc.asio.metadata.AtosMetadataService;
import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.metadata.RemoteMetadata;
import at.ac.univie.isc.asio.metadata.StaticMetadata;
import at.ac.univie.isc.asio.protocol.EndpointSupplier;
import at.ac.univie.isc.asio.protocol.EntryPoint;
import at.ac.univie.isc.asio.protocol.OperationParser;
import at.ac.univie.isc.asio.protocol.PrototypeEngineProvider;
import at.ac.univie.isc.asio.transport.JdkPipeTransferFactory;
import at.ac.univie.isc.asio.transport.ObservableStream;
import at.ac.univie.isc.asio.transport.ObservableStreamBodyWriter;
import at.ac.univie.isc.asio.transport.Transfer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.MessageBodyWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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

  @Autowired
  private Environment env;
  @Autowired
  @Qualifier("asio.meta.id")
  private Supplier<String> datasetIdResolver;

  // asio backend components

  @Autowired
  private Set<DatasetEngine> engines;

  @PostConstruct
  public void reportProfile() {
    final String[] profiles =
        env.getActiveProfiles().length == 0
          ? env.getDefaultProfiles()
          : env.getActiveProfiles();
    log.info("[BOOT] active profiles : {}", Arrays.toString(profiles));
  }

  // JAX-RS service endpoints

  @Bean(name = "asio_entry")
  public EntryPoint entryPoint() {
    return new EntryPoint(endpointSupplier(), metadataSupplier());
  }

  // JAX-RS provider

  @Bean(name = "asio_error_mapper")
  public DatasetExceptionMapper errorMapper() {
    return new DatasetExceptionMapper();
  }

  @Bean(name = "asio_stream_writer")
  public MessageBodyWriter<ObservableStream> streamWriter() {
    return new ObservableStreamBodyWriter();
  }

  @Bean(name = "asio_log_filter")
  public LogContextFilter logFilter() {
    return new LogContextFilter();
  }

  @Bean(name = "asio_accept_tunnel")
  public AcceptTunnelFilter tunnelFilter() {
    return new AcceptTunnelFilter();
  }

  @Bean(name = "asio_accept_defaults")
  public ContentNegotiationDefaultsFilter defaultsFilter() {
    return new ContentNegotiationDefaultsFilter(); }

  @Bean(name = "json_serializer")
  public JSONProvider jsonSerializer() {
    final JSONProvider provider = new JSONProvider();
    provider.setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio/metadata", "asio"));
    return provider;
  }

  // asio frontend components

  @Bean
  public EndpointSupplier endpointSupplier() {
    log.info("[BOOT] using engines {}", engines);
    return new PrototypeEngineProvider(engines, parser(), processor(), transferFactory(), responseExecutor(), globalTimeout());
  }

  // default resolver
  @Bean
  @Qualifier("asio.meta.id")
  public Supplier<String> environmentDatasetIdResolver() {
    return new Supplier<String>() {
      @Override
      public String get() {
        return env.getRequiredProperty("asio.meta.id");
      }
    };
  }

  @Bean
  public TimeoutSpec globalTimeout() {
    Long timeout = env.getProperty("asio.timeout", Long.class, Long.valueOf(-1L));
    TimeoutSpec spec = TimeoutSpec.from(timeout.longValue(), TimeUnit.SECONDS);
    log.info("[BOOT] using timeout {}", spec);
    return spec;
  }

  // TODO let engine configurations create the metadata supplier
  @Bean
  public Supplier<DatasetMetadata> metadataSupplier() {
    final boolean contactRemote = env.getProperty("asio.meta.enable", Boolean.class, Boolean.FALSE);
    if (env.acceptsProfiles("federation")) {  // FIXME ignore setting if federation node
      log.info("[BOOT] using federation node metadata");
      return Suppliers.ofInstance(StaticMetadata.FEDERATION_NODE);
    } else if (contactRemote) {
      final URI repository = URI.create(env.getRequiredProperty("asio.meta.repository"));
      AtosMetadataService proxy = new AtosMetadataService(repository);
      final String datasetId = datasetIdResolver.get();
      log.info("[BOOT] using metadata service {} with id {}", proxy, datasetId);
      return new RemoteMetadata(proxy, datasetId);
    } else {
      log.info("[BOOT] metadata resolution disabled");
      return Suppliers.ofInstance(StaticMetadata.NOT_AVAILABLE);
    }
  }

  @Bean
  public OperationParser parser() {
    final IdGenerator ids = RandomIdGenerator.withPrefix("asio");
    final OperationFactory factory = new OperationFactory(ids);
    return new OperationParser(factory);
  }

  @Bean
  public AsyncProcessor processor() {
    return new ListeningAsyncProcessor(responseExecutor(), converter()).withTimeout(globalTimeout());
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
