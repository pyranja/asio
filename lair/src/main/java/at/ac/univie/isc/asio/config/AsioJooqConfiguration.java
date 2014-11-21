package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.engine.sql.MySqlSchemaProvider;
import at.ac.univie.isc.asio.engine.sql.H2SchemaProvider;
import at.ac.univie.isc.asio.tool.DatasourceSpec;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Supplier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsioJooqConfiguration {
  private static final int CONCURRENCY = 5;

  @Bean
  public JooqEngine sqlEngine(final DataSource pool, final DatasourceSpec sqlSpec, final TimeoutSpec timeout) {
    final SQLDialect dialect = JDBCUtils.dialect(sqlSpec.getJdbcUrl());
    final DSLContext jooq = DSL.using(pool, dialect);
    return new JooqEngine(jooq, timeout);
  }

  @Bean
  @Qualifier("asio.meta.schema")
  public Supplier<SqlSchema> schemaSupplier(final DataSource pool) throws SQLException {
    final SQLDialect dialect = JDBCUtils.dialect(pool.getConnection());
    switch (dialect) {
      case MYSQL: return new MySqlSchemaProvider(pool);
      default : return new H2SchemaProvider(pool);
    }
  }

  @Bean(destroyMethod = "close")
  public DataSource connectionPool(final DatasourceSpec sqlSpec, final TimeoutSpec timeout) {
    final HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(CONCURRENCY);
    config.setDriverClassName(sqlSpec.getJdbcDriver());
    config.setJdbcUrl(sqlSpec.getJdbcUrl());
    config.setUsername(sqlSpec.getUsername());
    config.setPassword(sqlSpec.getPassword());
    config.setConnectionTimeout(timeout.getAs(TimeUnit.MILLISECONDS, 0L));
    return new HikariDataSource(config);
  }
}
