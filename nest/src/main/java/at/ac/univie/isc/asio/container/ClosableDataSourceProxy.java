package at.ac.univie.isc.asio.container;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * TODO : needed as JooqEngine has to strict type requirements.
 */
class ClosableDataSourceProxy implements DataSource, AutoCloseable {
  static ClosableDataSourceProxy wrap(final DataSource delegate) {
    return new ClosableDataSourceProxy(delegate);
  }

  private final DataSource delegate;

  private ClosableDataSourceProxy(final DataSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public void close() throws Exception {
    if (delegate instanceof AutoCloseable) {
      ((AutoCloseable) delegate).close();
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return delegate.getConnection();
  }

  @Override
  public Connection getConnection(final String username, final String password) throws SQLException {
    return delegate.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    if (isWrapperFor(iface)) {
      return iface.cast(delegate);
    } else {
      throw new SQLException("cannot cast proxied " + delegate + " to " + iface);
    }
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return iface.isAssignableFrom(delegate.getClass());
  }
}
