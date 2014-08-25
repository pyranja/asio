package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.EngineRegistry;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.*;
import at.ac.univie.isc.asio.engine.ProtocolResource;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

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
  @Autowired(required = false)
  @Qualifier("asio.meta.schema")
  private Supplier<SqlSchema> schemaSource = Suppliers.ofInstance(new SqlSchema());

  // asio backend components

  @Autowired
  private Set<Engine> engines = Collections.emptySet();

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
    return new ProtocolResource(registry(), globalTimeout());
  }

  @Bean
  public MetadataResource metadataResource() {
    return new MetadataResource(metadataSupplier(), schemaSource);
  }

  // asio jaxrs components
  @Bean
  public Command.Factory registry() {
    log.info("[BOOT] using engines {}", engines);
    final Scheduler scheduler = Schedulers.from(workerPool());
    return new EngineRegistry(scheduler, engines);
  }

  @Bean(destroyMethod = "shutdownNow")
  public ExecutorService workerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("asio-worker-%d")
            .build();
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(30);
    return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue, factory);
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
    Long timeout = env.getProperty("asio.timeout", Long.class, -1L);
    TimeoutSpec spec = TimeoutSpec.from(timeout, TimeUnit.SECONDS);
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
}
