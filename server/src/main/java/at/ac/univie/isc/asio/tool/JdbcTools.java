package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.database.Jdbc;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;

import java.util.Map;
import java.util.Properties;

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
   * Customize hikari configuration in place, using a jdbc settings holder.
   *
   * @param hikari origin hikari settings
   * @param id     id of the enclosing context
   * @param jdbc   jdbc settings
   * @return customized hikari configuration
   */
  public static HikariConfig populate(final HikariConfig hikari, final String id, final Jdbc jdbc) {
    hikari.setCatalog(jdbc.getSchema());

    hikari.setJdbcUrl(jdbc.getUrl());
    hikari.setUsername(jdbc.getUsername());
    hikari.setPassword(jdbc.getPassword());
    if (jdbc.getDriver() != null && hikari.getDataSourceClassName() == null) {
      // only set the driver class if no DataSource class is given - hikari will not accept both
      hikari.setDriverClassName(jdbc.getDriver());
    }
    final Properties properties = injectRequiredProperties(jdbc.getProperties(), jdbc.getUrl());
    hikari.setDataSourceProperties(properties);

    final String poolName = Pretty.format("%s-hikari-pool", id);
    hikari.setPoolName(poolName);
    hikari.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(
        poolName + "-thread-%d").build());

    return hikari;
  }

  static Properties injectRequiredProperties(final Map<String, String> original,
                                             final String url) {
    final Properties result = Beans.asProperties(original);
    if (url.startsWith("jdbc:h2:")) {
      result.setProperty("MODE", "MYSQL");
      result.setProperty("DATABASE_TO_UPPER", "false");
    }
    return result;
  }
}
