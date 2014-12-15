package at.ac.univie.isc.asio.sql;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Obtain connections from the JDK built-in {@code DriverManager}. Connections are not reused.
 * This implementation is a port of {@code com.zaxxer.hikari.util.DriverDataSource}
 * functionality from the Hikari connection pool.
 *
 * @see java.sql.DriverManager
 */
@ThreadSafe
public class DriverManagerDataSource implements DataSource, AutoCloseable {
  private final String jdbcUrl;
  private final Properties defaults;

  public DriverManagerDataSource(@Nonnull final String jdbcUrl, @Nonnull final Properties settings)
      throws IllegalArgumentException {
    this.jdbcUrl = requireNonNull(jdbcUrl);
    // must clone, as HashTable methods on Properties do not honor defaults
    this.defaults = (Properties) requireNonNull(settings).clone();
    checkJdbcUrl(jdbcUrl);
  }

  private void checkJdbcUrl(final String jdbcUrl) {
    final String message = "No driver found for JDBC URL <" + jdbcUrl + ">";
    try {
      if (DriverManager.getDriver(jdbcUrl) == null) {
        throw new IllegalArgumentException(message);
      }
    } catch (SQLException e) {
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Delegates to {@link java.sql.DriverManager#getConnection(String, Properties)},
   * using the driver properties given on creation.
   */
  @Nonnull
  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, defaults);
  }

  /**
   * Delegates to {@link java.sql.DriverManager#getConnection(String, java.util.Properties)}, but
   * overrides the {@code user} and {@code password} property in the stored properties.
   */
  @Nonnull
  @Override
  public Connection getConnection(@Nonnull final String username, @Nonnull final String password)
      throws SQLException {
    final Properties cloned = (Properties) defaults.clone();
    cloned.setProperty("user", username);
    cloned.setProperty("password", password);
    return DriverManager.getConnection(jdbcUrl, cloned);
  }

  // Noop AutoCloseable implementation =============================================================

  @Override
  public void close() throws Exception { /* noop */ }

  // Delegated CommonDataSource implementation =====================================================

  /**
   * Delegates to {@link java.sql.DriverManager#getLogWriter()}.
   */
  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }

  /**
   * Delegates to {@link java.sql.DriverManager#setLogWriter(java.io.PrintWriter)}.
   */
  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    DriverManager.setLogWriter(out);
  }

  /**
   * Delegates to {@link java.sql.DriverManager#setLoginTimeout(int)}.
   */
  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    DriverManager.setLoginTimeout(seconds);
  }

  /**
   * Delegates to {@link java.sql.DriverManager#setLoginTimeout(int)}.
   */
  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  /**
   * {@code java.util.logging} is not supported.
   *
   * @return nothing - always fails
   * @throws SQLFeatureNotSupportedException always
   */
  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  // No-op Wrapper implementation ==================================================================

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return false;
  }
}
