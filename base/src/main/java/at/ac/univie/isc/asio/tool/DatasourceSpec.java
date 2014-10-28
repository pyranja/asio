package at.ac.univie.isc.asio.tool;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Container for JDBC connection properties.
 */
@Immutable
public final class DatasourceSpec {
  /**
   * Start building a spec with given JDBC URL.
   * @param jdbcUrl connection URL
   * @return fluent-interface builder
   */
  public static JdbcUrlHolder connectTo(final String jdbcUrl) {
    return new JdbcUrlHolder(jdbcUrl);
  }

  // jdbc
  private final String jdbcUrl;
  private final String jdbcDriver;
  // credentials
  private final String username;
  private final String password;

  @SuppressWarnings("ConstantConditions")
  DatasourceSpec(@Nonnull final String jdbcUrl,
                 @Nonnull final String jdbcDriver,
                 @Nullable final String username,
                 @Nullable final String password) {
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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("jdbcUrl", jdbcUrl)
        .add("jdbcDriver", jdbcDriver)
        .add("username", username)
        .toString();
  }

  /** Determine driver class from well known JDBC URLs */
  private static String guessJdbcDriver(final String url) {
    if (url.startsWith("jdbc:mysql")) {
      return "com.mysql.jdbc.Driver";
    } else if (url.startsWith("jdbc:h2")) {
      return "org.h2.Driver";
    } else {
      throw new IllegalStateException("database type not supported : "+ url);
    }
  }

  // ************** builder

  public static final class JdbcUrlHolder {
    private final String jdbcUrl;

    private JdbcUrlHolder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public JdbcConnectionHolder and() {
      return new JdbcConnectionHolder(jdbcUrl, guessJdbcDriver(jdbcUrl));
    }

    public JdbcConnectionHolder with(String jdbcDriver) {
      return new JdbcConnectionHolder(jdbcUrl, jdbcDriver);
    }
  }

  public static final class JdbcConnectionHolder {
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

    public DatasourceSpec noAuthentication() {
      return new DatasourceSpec(jdbcUrl, jdbcDriver, null, null);
    }
  }
}
