package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.d2rq.D2rqSpec;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.metadata.sql.H2SchemaService;
import at.ac.univie.isc.asio.metadata.sql.MysqlSchemaService;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;
import at.ac.univie.isc.asio.spring.SpringByteSource;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.zaxxer.hikari.HikariDataSource;
import org.d2rq.db.SQLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Configure a schema that is backed by a real database, providing a sql and sparql engine.
 */
@Configuration
@EnableConfigurationProperties(ContainerSettings.class)
public class SpringBluePrint {

  @Autowired
  private ContainerSettings config;

  @Bean(destroyMethod = "close")
  public JooqEngine jooqEngine(final DataSource pool, final TimeoutSpec timeout) {
    final JdbcSpec jdbc = JdbcSpec.connectTo(config.datasource.jdbcUrl)
        .authenticateAs(config.datasource.username, config.datasource.password)
        .use(timeout).complete();
    return JooqEngine.create(ClosableDataSourceProxy.wrap(pool), jdbc);
  }

  @Bean(destroyMethod = "close")
  public JenaEngine jenaEngine(final SQLConnection connection, final TimeoutSpec timeout, final ResourceLoader loader) {
    final Resource mappingSource = loader.getResource(config.sparql.d2rMappingLocation.toString());
    final SpringByteSource turtle = SpringByteSource.asByteSource(mappingSource);
    final Model model = D2rqSpec.load(turtle, config.sparql.d2rBaseUri).compile(connection);
    return JenaEngine.create(model, timeout, config.sparql.federation);
  }

  @Bean
  public RelationalSchemaService schemaService(final DataSource pool) {
    final String jdbcUrl = config.getDatasource().getJdbcUrl();
    if (jdbcUrl.startsWith("jdbc:mysql:")) {
      return new MysqlSchemaService(pool);
    } else if (jdbcUrl.startsWith("jdbc:h2:")) {
      return new H2SchemaService(pool);
    } else {
      throw new IllegalStateException(jdbcUrl + " not supported");
    }
  }

  @Bean(destroyMethod = "close")
  public SQLConnection d2rConnection() {
    // FIXME : use MySQL driver class name
    // jdbcUrl, driverClassName (optional), username, password
    return new SQLConnection(config.datasource.jdbcUrl, null, config.datasource.username, config.datasource.password);
  }

  @Bean(destroyMethod = "close")
  @ConfigurationProperties("nest.datasource")
  public DataSource dataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  @ConditionalOnProperty("nest.timeout")
  public TimeoutSpec timeout() {
    return TimeoutSpec.from(config.timeout, TimeUnit.MILLISECONDS);
  }
}
