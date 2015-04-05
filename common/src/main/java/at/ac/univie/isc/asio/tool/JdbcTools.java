package at.ac.univie.isc.asio.tool;

import com.google.common.base.Optional;

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
}
