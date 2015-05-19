/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.d2rq.pool.PooledD2rqFactory;
import at.ac.univie.isc.asio.database.DatabaseInspector;
import at.ac.univie.isc.asio.database.DefinitionService;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sparql.JenaFactory;
import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.metadata.DescriptorService;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.spring.ExplicitWiring;
import at.ac.univie.isc.asio.tool.JdbcTools;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.rdf.model.Model;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import rx.Observable;
import rx.functions.Func0;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@ExplicitWiring
class NestBluePrint {
  private static final Logger log = getLogger(NestBluePrint.class);

  static final String BEAN_DEFINITION_SOURCE = "definition";
  static final String BEAN_DESCRIPTOR_SOURCE = "metadata";
  static final String BEAN_MAPPING_SOURCE = "mapping";

  @Bean(destroyMethod = "close")
  public JooqEngine jooqEngine(final Jdbc jdbc,
                               final DataSource pool,
                               final Timeout timeout) {
    final JdbcSpec spec = JdbcSpec.connectTo(jdbc.getUrl())
        .authenticateAs(jdbc.getUrl(), jdbc.getPassword())
        .use(timeout).complete();
    return JooqEngine.create(ClosableDataSourceProxy.wrap(pool), spec);
  }

  @Bean(destroyMethod = "close")
  public JenaEngine jenaEngine(final Dataset dataset,
                               final D2rqConfigModel d2rq,
                               final Jdbc jdbc,
                               final Timeout timeout,
                               final Environment env) {
    final Integer poolSize = env.getProperty("asio.d2rq.pool-size", Integer.class, 1);
    final JenaFactory factory = PooledD2rqFactory.using(d2rq, jdbc, timeout, poolSize);
    return JenaEngine.using(factory, dataset.isFederationEnabled());
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

  @Bean(name = BEAN_MAPPING_SOURCE)
  public Observable<Model> mapping(final D2rqConfigModel d2rq) {
    return Observable.just(d2rq.getDefinition());
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
    return DatabaseInspector.create(jdbc.getUrl(), dataSource);
  }

  @Bean(destroyMethod = "close")
  @Primary
  public DataSource dataSource(final HikariConfig base,
                               final Dataset dataset,
                               final Jdbc jdbc,
                               final Timeout timeout) {
    final HikariConfig config = JdbcTools.populate(base, dataset.getName().asString(), jdbc);
    config.setConnectionTimeout(timeout.getAs(TimeUnit.MILLISECONDS, 0));
    return new HikariDataSource(config);
  }

  @Bean
  @Primary
  public Timeout localTimeout(final Dataset dataset, final Timeout global) {
    final Timeout local = dataset.getTimeout();
    log.debug(Scope.SYSTEM.marker(), "choosing timeout (local:{}) (global:{})", local, global);
    return local.orIfUndefined(global);
  }

  // fix mysql user management conflict on re-deployments
  // TODO formalize initializer || fix assembly/destruction inerleaving in another way

  @Autowired(required = false)
  private MysqlUserRepository mysql;
  @Autowired
  private Jdbc jdbc;

  @PostConstruct
  public void ensureMysqlUserPresent() {
    if (mysql != null) {
      final String schema = jdbc.getSchema();
      log.info(Scope.SYSTEM.marker(), "ensure mysql user for {} is present", schema);
      mysql.createUserFor(schema);
    }
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
