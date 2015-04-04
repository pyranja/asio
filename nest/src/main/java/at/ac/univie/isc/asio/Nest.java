package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.container.DescriptorService;
import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.insight.EventBusEmitter;
import at.ac.univie.isc.asio.metadata.AtosMetadataRepository;
import at.ac.univie.isc.asio.metadata.DescriptorConversion;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.spring.EventBusAutoRegistrar;
import at.ac.univie.isc.asio.spring.ExplicitWiring;
import at.ac.univie.isc.asio.spring.JerseyLogInitializer;
import at.ac.univie.isc.asio.spring.SpringAutoFactory;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Ticker;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {ManagementSecurityAutoConfiguration.class})
@EnableConfigurationProperties({AsioSettings.class})
@ComponentScan(
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SpringAutoFactory.class)
    , excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = ExplicitWiring.class)
)
public class Nest {

  public static void main(String[] args) {
    application().run(args);
  }

  static SpringApplicationBuilder application() {
    return new SpringApplicationBuilder(Nest.class).web(true).showBanner(false)
        .listeners(new JerseyLogInitializer());
  }

  @Bean
  public static EventBusAutoRegistrar eventBusAutoRegistrar(final EventBus eventBus) {
    return new EventBusAutoRegistrar(eventBus);
  }

  @Autowired
  private AsioSettings config;

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public Connector connector(final EngineRouter router,
                             final Authorizer authorizer,
                             final EventBus eventBus) {
    final ReactiveInvoker invoker = ReactiveInvoker.from(router, Schedulers.io(), authorizer);
    final EventBusEmitter emitter =
        EventBusEmitter.create(eventBus, Ticker.systemTicker(), at.ac.univie.isc.asio.Scope.REQUEST);
    return EventfulConnector.around(emitter, invoker);
  }

  @Bean
  public EventBus eventBus(final ScheduledExecutorService workerPool) {
    return new AsyncEventBus("asio-events", workerPool);
  }

  @Bean(destroyMethod = "shutdown")
  public ScheduledExecutorService workerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("asio-worker-%d").build();
    final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(factory);
    final DelegatingSecurityContextScheduledExecutorService secured =
        new DelegatingSecurityContextScheduledExecutorService(executor);
    return secured;
  }

  @Bean
  @ConditionalOnProperty(AsioFeatures.VPH_METADATA)
  public DescriptorService descriptorService(final Client http) {
    final WebTarget endpoint = http.target(config.getMetadataRepository());
    final AtosMetadataRepository atos = new AtosMetadataRepository(endpoint);
    return new DescriptorService() {
      @Override
      public Observable<SchemaDescriptor> metadata(final URI identifier) {
        return atos.findByLocalId(identifier.toString()).map(DescriptorConversion.asFunction());
      }
    };
  }

  @Bean(destroyMethod = "close")
  @Lazy
  public Client httpClient() {
    return ClientBuilder.newClient();
  }

  @Bean
  public Path workingDirectory() {
    return Paths.get(config.getHome());
  }

  @Bean
  public TimeoutSpec globalTimeout() {
    return TimeoutSpec.from(config.timeout, TimeUnit.MILLISECONDS);
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
  public DatasetHolder activeDataset() {
    return new DatasetHolder();
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES)
  public SecurityContext currentUser() {
    return SecurityContextHolder.getContext();
  }
}
