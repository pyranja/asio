package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.EventfulMysqlInterceptor;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.engine.DatasetHolder;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.tool.JdbcTools;
import at.ac.univie.isc.asio.tool.Timeout;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Paths;

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
  @ConditionalOnProperty(AsioFeatures.MULTI_TENANCY)
  public MysqlUserRepository userRepository(final DataSource datasource) {
    return new MysqlUserRepository(datasource, config.jdbc.getPrivileges());
  }

  @Bean
  @ConditionalOnProperty(AsioFeatures.GLOBAL_DATASOURCE)
  public DataSource globalDatasource(final Timeout timeout) {
    final HikariConfig config = JdbcTools.hikariConfig("global", this.config.jdbc, timeout);
    config.getDataSourceProperties()
        .setProperty("statementInterceptors", EventfulMysqlInterceptor.class.getName());
    config.setCatalog(null);
    // expect infrequent usage
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(2);
    return new HikariDataSource(config);
  }
}
