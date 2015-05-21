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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.engine.DatasetHolder;
import at.ac.univie.isc.asio.engine.sql.CommandWhitelist;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.tool.JdbcTools;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Predicate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Components required for the container management in brood.
 */
@Configuration
@Brood
class BroodComponents {

  @Autowired
  private AsioSettings config;

  @Bean
  public FileSystemConfigStore fileSystemConfigStore(final Timeout timeout) {
    return new FileSystemConfigStore(Paths.get(config.getHome()), timeout);
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
  public DatasetHolder activeDataset() {
    return new DatasetHolder();
  }

  @Bean
  @Primary
  public ResourceConfig broodJerseyConfiguration(final ResourceConfig jersey) {
    jersey.setApplicationName("jersey-brood");
    jersey.register(ApiResource.class);
    if (config.feature.isVphUriAuth()) {
      jersey.register(UriBasedRoutingResource.class);
    } else {
      jersey.register(DefaultRoutingResource.class);
    }
    return jersey;
  }

  @Bean
  public Predicate<String> sqlCommandWhitelist() {
    return CommandWhitelist.allowOnly(config.getJdbc().getAllowedCommands());
  }

  /** include custom properties in base-line config */
  @Bean
  @Primary  // required to override globalDatasource
  @ConfigurationProperties("asio.hikari")
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public HikariConfig baseHikariConfig(final Timeout timeout) {
    final HikariConfig config = new HikariConfig();
    config.setConnectionTimeout(timeout.getAs(TimeUnit.MILLISECONDS, 0));
    return config;
  }

  @Bean
  @ConditionalOnProperty(AsioFeatures.MULTI_TENANCY)
  public MysqlUserRepository userRepository(final DataSource datasource) {
    return new MysqlUserRepository(datasource, config.jdbc.getPrivileges());
  }

  @Bean
  @ConditionalOnProperty(AsioFeatures.GLOBAL_DATASOURCE)
  public DataSource globalDatasource(final HikariConfig base) {
    final HikariConfig config = JdbcTools.populate(base, "global", this.config.jdbc);
    config.setCatalog(null);
    // expect infrequent usage
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(2);
    return new HikariDataSource(config);
  }
}
