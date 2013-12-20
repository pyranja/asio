package at.ac.univie.isc.asio.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * Record connection informations for a DB backing asio.
 * 
 * @author Chris Borckholder
 */
public class DatasourceSpec {

  public static DatasourceSpecBuilder connectTo(final String jdbcUrl, final String jdbcDriver) {
    return new DatasourceSpecBuilder(jdbcDriver, jdbcUrl);
  }

  // jdbc
  private final String jdbcUrl;
  private final String jdbcDriver;
  // credentials
  private final String username;
  private final String password;

  public DatasourceSpec(final String jdbcUrl, final String jdbcDriver, final String username,
      final String password) {
    super();
    this.jdbcUrl = checkNotNull(emptyToNull(jdbcUrl), "no JDBC connection URL given");
    this.jdbcDriver = checkNotNull(emptyToNull(jdbcDriver), "missing driver class for %s", jdbcUrl);
    this.username = checkNotNull(emptyToNull(username), "missing username for %s", jdbcUrl);
    this.password = checkNotNull(emptyToNull(password), "missing password for %s", jdbcUrl);
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

  public static class DatasourceSpecBuilder {
    private final String jdbcUrl;
    private final String jdbcDriver;

    public DatasourceSpecBuilder(final String jdbcUrl, final String jdbcDriver) {
      super();
      this.jdbcDriver = jdbcDriver;
      this.jdbcUrl = jdbcUrl;
    }

    public DatasourceSpec authenticateAs(final String username, final String password) {
      return new DatasourceSpec(jdbcDriver, jdbcUrl, username, password);
    }
  }

  @Override
  public String toString() {
    return String.format("DatasourceSpec [jdbcUrl=%s, jdbcDriver=%s, username=%s, password=%s]",
        jdbcUrl, jdbcDriver, username, password);
  }
}
