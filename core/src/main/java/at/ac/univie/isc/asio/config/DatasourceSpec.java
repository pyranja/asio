package at.ac.univie.isc.asio.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Record connection informations for a DB backing asio.
 *
 * @author Chris Borckholder
 */
public class DatasourceSpec {

  public static JdbcUrlHolder connectTo(final String jdbcUrl) {
    return new JdbcUrlHolder(jdbcUrl);
  }

  // jdbc
  private final String jdbcUrl;
  private final String jdbcDriver;
  // credentials
  private final String username;
  private final String password;

  DatasourceSpec(final String jdbcUrl, final String jdbcDriver, final String username,
                        final String password) {
    super();
    checkNotNull(emptyToNull(jdbcUrl), "no JDBC connection URL given");
    this.jdbcUrl = addDefaultOptions(jdbcUrl);
    this.jdbcDriver = checkNotNull(emptyToNull(jdbcDriver), "missing driver class for %s", jdbcUrl);
    this.username = nullToEmpty(username);
    this.password = nullToEmpty(password);
  }

  private static final Pattern MYSQL_JDBC = Pattern.compile("^jdbc:mysql://.*$");
  private static final String MYSQL_DEFAULTS = "?zeroDateTimeBehavior=convertToNull";

  private String addDefaultOptions(final String jdbcUrl) {
    String processed = jdbcUrl;
    final Matcher mysqlPattern = MYSQL_JDBC.matcher(jdbcUrl);
    if (mysqlPattern.matches()) {
      processed = jdbcUrl + MYSQL_DEFAULTS;
    }
    return processed;
  }

  public String getJdbcDriver() {
    return jdbcDriver;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String driverName() {
    if (jdbcUrl.startsWith("jdbc:mysql")) {
      return "com.mysql.jdbc.Driver";
    } else if (jdbcUrl.startsWith("jdbc:h2")) {
      return "org.h2.Driver";
    } else {
      throw new IllegalStateException("database type not supported : "+ jdbcUrl);
    }
  }

  @Override
  public String toString() {
    return String.format("DatasourceSpec [jdbcUrl=%s, jdbcDriver=%s, username=%s]",
        jdbcUrl, jdbcDriver, username);
  }

  // ************** builder


  public static class JdbcUrlHolder {
    private final String jdbcUrl;

    private JdbcUrlHolder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public JdbcConnectionHolder with(String jdbcDriver) {
      return new JdbcConnectionHolder(jdbcUrl, jdbcDriver);
    }
  }


  public static class JdbcConnectionHolder {
    private final String jdbcUrl;
    private final String jdbcDriver;

    private JdbcConnectionHolder(final String jdbcUrl, final String jdbcDriver) {
      super();
      this.jdbcDriver = jdbcDriver;
      this.jdbcUrl = jdbcUrl;
    }

    public DatasourceSpec authenticateAs(final String username, final String password) {
      return new DatasourceSpec(jdbcUrl, jdbcDriver, username, password);
    }

    public DatasourceSpec anonymous() {
      return new DatasourceSpec(jdbcUrl, jdbcDriver, null, null);
    }
  }
}
