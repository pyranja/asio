package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.engine.sql.SchemaProvider;
import com.google.common.base.Supplier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
@Profile("dataset")
public class AsioJooqConfiguration {
  private static final int CONCURRENCY = 5;

  @Autowired
  private DatasourceSpec dbSpec;
  @Autowired
  private TimeoutSpec globalTimeout;

  @Bean
  public JooqEngine sqlEngine() {
    final SQLDialect dialect = JDBCUtils.dialect(dbSpec.getJdbcUrl());
    final DSLContext jooq = DSL.using(dataSource(), dialect);
    return new JooqEngine(jooq, globalTimeout);
  }

  @Bean
  @Qualifier("asio.meta.schema")
  public Supplier<SqlSchema> schemaSupplier() {
    return new SchemaProvider(dataSource(), SchemaProvider.H2_INTERNAL_SCHEMA);
  }

  @Bean(destroyMethod = "close")
  public DataSource dataSource() {
    final HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(CONCURRENCY);
    config.setDriverClassName(dbSpec.driverName());
    config.setJdbcUrl(dbSpec.getJdbcUrl());
    config.setUsername(dbSpec.getUsername());
    config.setPassword(dbSpec.getPassword());
    config.setConnectionTimeout(globalTimeout.getAs(TimeUnit.MILLISECONDS, 0L));
    return new HikariDataSource(config);
  }
}
