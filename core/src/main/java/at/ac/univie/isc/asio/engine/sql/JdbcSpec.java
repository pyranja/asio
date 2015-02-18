package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Objects;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * Container for JDBC connection properties.
 */
@Immutable
public final class JdbcSpec {
  /**
   * Start building a spec with given JDBC URL.
   *
   * @param jdbcUrl connection URL
   * @return fluent-interface builder
   */
  public static Builder connectTo(@Nonnull final String jdbcUrl) {
    return new Builder(jdbcUrl);
  }

  static final Identity ANONYMOUS_ACCESS = Identity.from("", "");

  private final String url;
  private final String driver;
  private final Identity credentials;
  private final TimeoutSpec timeout;

  private JdbcSpec(@Nonnull final String url, @Nonnull final String driver,
                   @Nonnull final TimeoutSpec timeout, @Nonnull final Identity credentials) {
    checkNotNull(emptyToNull(url), "no JDBC connection URL given");
    this.url = addDefaultOptions(url);
    this.driver = checkNotNull(emptyToNull(driver), "missing driver class for %s", url);
    this.timeout = checkNotNull(timeout, "missing timeout for %s", url);
    this.credentials = checkNotNull(credentials, "missing credentials for %s", url).orIfUndefined(ANONYMOUS_ACCESS);
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

  /**
   * The JDBC connection url.
   *
   * @return jdbc url
   */
  public String getUrl() {
    return url;
  }

  /**
   * The jdbc driver class name.
   *
   * @return class name
   */
  public String getDriver() {
    return driver;
  }

  /**
   * The timeout of jdbc requests.
   *
   * @return timeout value
   */
  public TimeoutSpec getTimeout() {
    return timeout;
  }

  /**
   * default username for jdbc connections.
   *
   * @return username
   */
  public String getUsername() {
    return credentials.getName();
  }

  /**
   * default password for jdbc connections.
   *
   * @return password
   */
  public String getPassword() {
    return credentials.getSecret();
  }

  /**
   * default jdbc connection credentials.
   *
   * @return credentials
   */
  public Identity getCredentials() {
    return credentials;
  }

  /**
   * Guess sql dialect from jdbc url
   *
   * @return inferred jooq dialect
   */
  public SQLDialect getDialect() {
    return JDBCUtils.dialect(url);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("url", url)
        .add("driver", driver)
        .add("dialect", getDialect())
        .add("timeout", timeout)
        .add("username", credentials)
        .toString();
  }

  /**
   * Determine driver class from well known JDBC URLs
   */
  static String jdbcDriverFor(final String url) {
    if (url.startsWith("jdbc:mysql")) {
      return "com.mysql.jdbc.Driver";
    } else if (url.startsWith("jdbc:h2")) {
      return "org.h2.Driver";
    } else {
      throw new IllegalArgumentException("cannot infer driver class for <" + url + ">");
    }
  }

  // ************** builder

  public static final class Builder {
    private final String jdbcUrl;
    private String jdbcDriver;
    private Identity credentials = Identity.undefined();
    private TimeoutSpec timeout = TimeoutSpec.undefined();

    private Builder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public Builder with(final String jdbcDriver) {
      this.jdbcDriver = jdbcDriver;
      return this;
    }

    public Builder authenticateAs(final String username, final String password) {
      this.credentials = Identity.from(username, password);
      return this;
    }

    public Builder use(final TimeoutSpec timeout) {
      this.timeout = timeout;
      return this;
    }

    public JdbcSpec complete() {
      if (jdbcDriver == null) { jdbcDriver = jdbcDriverFor(jdbcUrl); }
      return new JdbcSpec(jdbcUrl, jdbcDriver, timeout, credentials);
    }
  }
}
