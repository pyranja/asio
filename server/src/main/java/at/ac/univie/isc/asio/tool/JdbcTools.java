package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.database.Jdbc;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for working with JDBC.
 */
public final class JdbcTools {
  private JdbcTools() { /* static helpers */ }

  /**
   * Determine whether the given {@code String} is a valid jdbc url.
   *
   * @param url the string to check
   * @return true if the string seems to be a jdbc
   */
  public static boolean isValidJdbcUrl(final String url) {
    return url != null && url.startsWith("jdbc:");
  }

  static final Optional<String> MYSQL_DRIVER = Optional.of("com.mysql.jdbc.Driver");
  static final Optional<String> H2_DRIVER = Optional.of("org.h2.Driver");

  /**
   * Attempt to find the classname of a {@code Driver}, which supports the given jdbc url.
   *
   * @param url the jdbc url
   * @return the driver class supporting the given jdbc url or nothing if unknown
   */
  public static Optional<String> inferDriverClass(final String url) {
    Optional<String> result = Optional.absent();
    if (url != null) {
      if (url.startsWith("jdbc:mysql:")) {
        result = MYSQL_DRIVER;
      } else if (url.startsWith("jdbc:h2:")) {
        result = H2_DRIVER;
      }
    }
    return result;
  }

  /**
   * Create a hikari pool configuration holder from the given beans.
   *
   * @param id identifier of the pool, mainly for diagnostics
   * @param jdbc jdbc connection settings
   * @param timeout timeout of connections
   * @return mutable hikari settings holder
   */
  public static HikariConfig hikariConfig(final String id, final Jdbc jdbc, final Timeout timeout) {
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
    final String poolName = Pretty.format("%s-hikari-pool", id);
    config.setPoolName(poolName);
    config.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(poolName + "-thread-%d").build());
    return config;
  }
}
