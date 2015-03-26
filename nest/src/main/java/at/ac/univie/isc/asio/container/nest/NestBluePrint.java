package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.Pretty;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.container.ClosableDataSourceProxy;
import at.ac.univie.isc.asio.container.DefinitionService;
import at.ac.univie.isc.asio.container.DescriptorService;
import at.ac.univie.isc.asio.d2rq.D2rqTools;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.metadata.sql.DatabaseInspector;
import at.ac.univie.isc.asio.spring.ExplicitWiring;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.d2rq.db.SQLConnection;
import org.d2rq.lang.Mapping;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import rx.Observable;
import rx.functions.Func0;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@ExplicitWiring
class NestBluePrint {
  private static final Logger log = getLogger(NestBluePrint.class);

  static final String BEAN_DEFINITION_SOURCE = "definition";
  static final String BEAN_DESCRIPTOR_SOURCE = "metadata";

  @Bean(destroyMethod = "close")
  public JooqEngine jooqEngine(final Jdbc jdbc,
                               final DataSource pool,
                               final TimeoutSpec timeout) {
    final JdbcSpec spec = JdbcSpec.connectTo(jdbc.getUrl())
        .authenticateAs(jdbc.getUrl(), jdbc.getPassword())
        .use(timeout).complete();
    return JooqEngine.create(ClosableDataSourceProxy.wrap(pool), spec);
  }

  @Bean(destroyMethod = "close")
  public JenaEngine jenaEngine(final Dataset dataset,
                               final Mapping mapping,
                               final SQLConnection connection,
                               final TimeoutSpec timeout) {
    final Model model = D2rqTools.compile(mapping, connection);
    return JenaEngine.create(model, timeout, dataset.isFederationEnabled());
  }

  @Bean(name = BEAN_DEFINITION_SOURCE)
  public Observable<SqlSchema> definition(final Jdbc jdbc,
                                          final DefinitionService definitionService) {
    return Observable.defer(new CallDefinitionService(definitionService, jdbc.getSchema()));
  }

  @Bean(name = BEAN_DESCRIPTOR_SOURCE)
  public Observable<SchemaDescriptor> metadata(final Dataset dataset,
                                               final DescriptorService descriptorService) {
    return Observable.defer(new CallDescriptorService(descriptorService, dataset.getIdentifier()));
  }

  @Bean
  @ConditionalOnMissingBean
  public DescriptorService nullDescriptorService() {
    log.info(Scope.SYSTEM.marker(), "creating static fallback DescriptorService");
    return new DescriptorService() {
      @Override
      public Observable<SchemaDescriptor> metadata(final URI identifier) {
        return Observable.empty();
      }
    };
  }

  @Bean
  @ConditionalOnMissingBean
  public DefinitionService localDefinitionService(final Jdbc jdbc, final DataSource dataSource) {
    log.info(Scope.SYSTEM.marker(), "creating local DefinitionService from {}", jdbc);
    return DatabaseInspector.nonFixedSchema(jdbc.getUrl(), dataSource);
  }

  @Bean(destroyMethod = "close")
  public SQLConnection d2rqConnection(final Jdbc jdbc) {
    return new SQLConnection(jdbc.getUrl(), jdbc.getDriver(), jdbc.getUsername(), jdbc.getPassword());
  }

  @Bean(destroyMethod = "close")
  public DataSource dataSource(final Dataset dataset,
                               final Jdbc jdbc,
                               final TimeoutSpec timeout) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbc.getUrl());
    config.setCatalog(jdbc.getSchema());
    config.setUsername(jdbc.getUsername());
    config.setPassword(jdbc.getPassword());
    final Properties properties = new Properties();
    properties.putAll(jdbc.getProperties());
    config.setDataSourceProperties(properties);
    if (jdbc.getDriver() != null) { config.setDriverClassName(jdbc.getDriver()); }
    config.setConnectionTimeout(timeout.getAs(TimeUnit.MILLISECONDS, 0));
    final String poolName = Pretty.format("%s-hikari-pool", dataset.getName());
    config.setPoolName(poolName);
    config.setThreadFactory(new ThreadFactoryBuilder()
        .setNameFormat(poolName + "-thread-%d").build());
    return new HikariDataSource(config);
  }

  @Bean
  @Primary
  public TimeoutSpec localTimeout(final Dataset dataset, final TimeoutSpec global) {
    final TimeoutSpec local = dataset.getTimeout();
    log.debug(Scope.SYSTEM.marker(), "choosing timeout (local:{}) (global:{})", local, global);
    return local.orIfUndefined(global);
  }

  // Observable factories as nest static classes to avoid inner classes with implicit references.


  private static class CallDescriptorService implements Func0<Observable<? extends SchemaDescriptor>> {
    private final DescriptorService service;
    private final URI identifier;

    public CallDescriptorService(final DescriptorService serviceRef, final URI identifierRef) {
      this.service = serviceRef;
      this.identifier = identifierRef;
    }

    @Override
    public Observable<? extends SchemaDescriptor> call() {
      return service.metadata(identifier);
    }
  }


  private static class CallDefinitionService implements Func0<Observable<? extends SqlSchema>> {
    private final DefinitionService service;
    private final String schema;

    public CallDefinitionService(final DefinitionService serviceRef, final String schemaRef) {
      this.service = serviceRef;
      this.schema = schemaRef;
    }

    @Override
    public Observable<? extends SqlSchema> call() {
      return service.definition(schema);
    }
  }
}
