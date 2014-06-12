package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.protocol.PrototypeConnectorRegistry;
import at.ac.univie.isc.asio.tool.IdGenerator;
import at.ac.univie.isc.asio.tool.RandomIdGenerator;
import at.ac.univie.isc.asio.engine.OperationFactory;
import at.ac.univie.isc.asio.tool.VariantConverter;
import at.ac.univie.isc.asio.metadata.*;
import at.ac.univie.isc.asio.protocol.ProtocolResource;
import at.ac.univie.isc.asio.transport.JdkPipeTransferFactory;
import at.ac.univie.isc.asio.transport.Transfer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.RuntimeDelegate;
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

  // JAX-RS
  @Bean(destroyMethod = "shutdown")
  public Bus cxf() {
    return new SpringBus();
  }

  @Bean(destroyMethod = "stop")
  @DependsOn("cxf")
  public Server jaxrsServer() {
    final JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(
        JaxrsSpec.create(ProtocolResource.class, MetadataResource.class),
        JAXRSServerFactoryBean.class
    );
    log.info("[BOOT] publishing jaxrs endpoint at <{}>", factory.getAddress());
    // Use spring managed resource instances
    factory.setResourceProvider(ProtocolResource.class, protocolResourceProvider());
    factory.setResourceProvider(MetadataResource.class, metadataResourceProvider());
    factory.getFeatures().add(new LoggingFeature());  // TODO make configurable
    return factory.create();
  }

  @Bean
  public SpringResourceFactory protocolResourceProvider() {
    return new SpringResourceFactory("protocolResource");
  }

  @Bean
  public SpringResourceFactory metadataResourceProvider() {
    return new SpringResourceFactory("metadataResource");
  }

  @Bean
  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  public ProtocolResource protocolResource() {
    return new ProtocolResource(endpointSupplier(), globalTimeout());
  }

  @Bean
  public MetadataResource metadataResource() {
    return new MetadataResource(metadataSupplier());
  }

  // asio jaxrs components

  @Bean
  public PrototypeConnectorRegistry endpointSupplier() {
    log.info("[BOOT] using engines {}", engines);
    return new PrototypeConnectorRegistry(engines, transferFactory(), responseExecutor(), operationFactory());
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
  public OperationFactory operationFactory() {
    final IdGenerator ids = RandomIdGenerator.withPrefix("asio");
    return new OperationFactory(ids);
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
