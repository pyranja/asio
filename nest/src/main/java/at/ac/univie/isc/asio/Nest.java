package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.container.Schema;
import at.ac.univie.isc.asio.container.SchemaFactory;
import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.insight.EventBusEmitter;
import at.ac.univie.isc.asio.insight.EventLoggerBridge;
import at.ac.univie.isc.asio.metadata.MetadataService;
import at.ac.univie.isc.asio.metadata.NullMetadataService;
import at.ac.univie.isc.asio.metadata.sql.H2SchemaService;
import at.ac.univie.isc.asio.metadata.sql.MysqlSchemaService;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;
import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.spring.EventBusAutoRegistrator;
import at.ac.univie.isc.asio.spring.JerseyLogInitializer;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Ticker;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import rx.schedulers.Schedulers;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@PropertySource("${nest.configuration}")
@EnableConfigurationProperties(AsioSettings.class)
public class Nest {

  public static void main(String[] args) {
    application().run(args);
  }

  static SpringApplicationBuilder application() {
    return new SpringApplicationBuilder(Nest.class).web(true).showBanner(false)
        .listeners(new JerseyLogInitializer());
  }

  @Bean
  public static EventBusAutoRegistrator eventBusAutoRegistrator(final EventBus eventBus) {
    return new EventBusAutoRegistrator(eventBus);
  }

  @Autowired
  private AsioSettings config;

  @Bean
  public Schema defaultSchema(final SchemaFactory factory,
                              @Value("${nest.configuration}") final Resource schemaConfig) throws IOException {
    return factory.create(new ResourcePropertySource(schemaConfig));
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public Connector connector(final Schema schema,
                             final Authorizer authorizer,
                             final EventBus eventBus) {
    final EngineRouter router = FixedSelection.from(schema.engines());
    final ReactiveInvoker invoker = ReactiveInvoker.from(router, Schedulers.io(), authorizer);
    final EventBusEmitter emitter =
        EventBusEmitter.create(eventBus, Ticker.systemTicker(), at.ac.univie.isc.asio.Scope.REQUEST);
    return EventfulConnector.around(emitter, invoker);
  }

  @Bean
  public MetadataService metadataService() {
    return new NullMetadataService();
  }

  @Bean
  public RelationalSchemaService schemaService(@Value("${asio.db.jdbc-url}") final String jdbcUrl,
                                               final DataSource pool) {
    if (jdbcUrl.startsWith("jdbc:mysql:")) {
      return new MysqlSchemaService(pool);
    } else if (jdbcUrl.startsWith("jdbc:h2:")) {
      return new H2SchemaService(pool);
    } else {
      throw new IllegalStateException(jdbcUrl + " not supported");
    }
  }

  @Bean(destroyMethod = "close")
  @ConfigurationProperties("asio.db")
  public DataSource dataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  public EventBus eventBus(final ScheduledExecutorService workerPool) {
    final AsyncEventBus eventBus = new AsyncEventBus("asio-events", workerPool);
    eventBus.register(new EventLoggerBridge());
    return eventBus;
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
  public TimeoutSpec timeout() {
    return TimeoutSpec.from(config.timeout, TimeUnit.MILLISECONDS);
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES)
  public SecurityContext currentUser() {
    return SecurityContextHolder.getContext();
  }
}
