package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.admin.EventLoggerBridge;
import at.ac.univie.isc.asio.admin.EventStream;
import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.metadata.*;
import at.ac.univie.isc.asio.tool.Duration;
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
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;
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
  private static final Logger log = LoggerFactory.getLogger(AsioConfiguration.class);
  public static final Marker SYSTEM = MarkerFactory.getMarker("SYSTEM");

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
    log.info(SYSTEM, "active profiles : {}", Arrays.toString(profiles));
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
    log.info(SYSTEM, "publishing jaxrs endpoint at <{}>", factory.getAddress());
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
    log.info(SYSTEM, "using engines {}", engines);
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
    log.info(SYSTEM, "using timeout {}", spec);
    return spec;
  }

  // TODO let engine configurations create the metadata supplier
  @Bean
  public Supplier<DatasetMetadata> metadataSupplier() {
    final boolean contactRemote = env.getProperty("asio.meta.enable", Boolean.class, Boolean.FALSE);
    if (env.acceptsProfiles("federation")) {  // FIXME ignore setting if federation node
      log.info(SYSTEM, "using federation node metadata");
      return Suppliers.ofInstance(StaticMetadata.FEDERATION_NODE);
    } else if (contactRemote) {
      final URI repository = URI.create(env.getRequiredProperty("asio.meta.repository"));
      AtosMetadataService proxy = new AtosMetadataService(repository);
      final String datasetId = datasetIdResolver.get();
      log.info(SYSTEM, "using metadata service {} with id {}", proxy, datasetId);
      return new RemoteMetadata(proxy, datasetId);
    } else {
      log.info(SYSTEM, "metadata resolution disabled");
      return Suppliers.ofInstance(StaticMetadata.NOT_AVAILABLE);
    }
  }
}
