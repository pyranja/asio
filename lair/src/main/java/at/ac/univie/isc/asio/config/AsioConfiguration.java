package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.EngineRegistry;
import at.ac.univie.isc.asio.engine.EventReporter;
import at.ac.univie.isc.asio.engine.EventfulCommandDecorator;
import at.ac.univie.isc.asio.engine.ProtocolResource;
import at.ac.univie.isc.asio.insight.EventLoggerBridge;
import at.ac.univie.isc.asio.insight.EventStream;
import at.ac.univie.isc.asio.jaxrs.AppSpec;
import at.ac.univie.isc.asio.metadata.AtosMetadataService;
import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.metadata.MetadataResource;
import at.ac.univie.isc.asio.metadata.RemoteMetadata;
import at.ac.univie.isc.asio.metadata.StaticMetadata;
import at.ac.univie.isc.asio.tool.Duration;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Ticker;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
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
  private static final Logger log = LoggerFactory.getLogger(AsioConfiguration.class);
  private static final Marker SCOPE_SYSTEM = at.ac.univie.isc.asio.Scope.SYSTEM.marker();

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

  // JAX-RS
  @Bean(destroyMethod = "shutdown")
  public Bus cxf() {
    return new SpringBus();
  }

  @Bean(destroyMethod = "stop")
  @DependsOn("cxf")
  public Server jaxrsServer() {
    final JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(
        AppSpec.create(ProtocolResource.class, MetadataResource.class),
        JAXRSServerFactoryBean.class
    );
    log.info(SCOPE_SYSTEM, "publishing jaxrs endpoint at <{}>", factory.getAddress());
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
    return new ProtocolResource(registry(), globalTimeout(), eventBuilder());
  }

  @Bean
  public MetadataResource metadataResource() {
    return new MetadataResource(metadataSupplier(), schemaSource);
  }

  // asio jaxrs components
  @Bean
  public Command.Factory registry() {
    log.info(SCOPE_SYSTEM, "using engines {}", engines);
    final Scheduler scheduler = Schedulers.from(workerPool());
    final EngineRegistry engineRegistry = new EngineRegistry(scheduler, engines);
    return new EventfulCommandDecorator(engineRegistry, eventBuilder());
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
  public Supplier<EventReporter> eventBuilder() {
    final EventReporter eventReporter = new EventReporter(eventBus(), Ticker.systemTicker());
    return Suppliers.ofInstance(eventReporter);
  }

  @Bean
  public EventStream eventStream() {
    final EventStream stream =
        new EventStream(Schedulers.io(), Duration.create(1L, TimeUnit.SECONDS), 50);
    eventBus().register(stream);
    return stream;
  }

  @Bean
  public EventLoggerBridge eventLogger() {
    final EventLoggerBridge bridge = new EventLoggerBridge();
    eventBus().register(bridge);
    return bridge;
  }

  @Bean
  public EventBus eventBus() {
    return new AsyncEventBus("asio-events", eventWorker());
  }

  @Bean(destroyMethod = "shutdownNow")
  public ExecutorService eventWorker() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder()
            .setNameFormat("asio-events-%d")
            .build();
    return Executors.newSingleThreadScheduledExecutor(factory);
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
    log.info(SCOPE_SYSTEM, "using timeout {}", spec);
    return spec;
  }

  // TODO let engine configurations create the metadata supplier
  @Bean
  public Supplier<DatasetMetadata> metadataSupplier() {
    final boolean contactRemote = env.getProperty("asio.meta.enable", Boolean.class, Boolean.FALSE);
    if (contactRemote) {
      final URI repository = URI.create(env.getRequiredProperty("asio.meta.repository"));
      final AtosMetadataService proxy = new AtosMetadataService(repository);
      final String datasetId = datasetIdResolver.get();
      log.info(SCOPE_SYSTEM, "using metadata service {} with id {}", proxy, datasetId);
      return new RemoteMetadata(proxy, datasetId);
    } else {
      log.info(SCOPE_SYSTEM, "metadata resolution disabled");
      return Suppliers.ofInstance(StaticMetadata.NOT_AVAILABLE);
    }
  }
}
