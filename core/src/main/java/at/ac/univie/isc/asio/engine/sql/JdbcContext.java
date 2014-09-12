package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.tool.Resources;
import com.google.common.base.Objects;
import org.jooq.ConnectionProvider;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * Manage a SQL execution.
 */
final class JdbcContext implements AutoCloseable {
  private final DSLContext jooq;
  private final ConnectionProvider pool;
  private final TimeoutSpec timeout;

  private Connection connection;
  private Statement statement;

  JdbcContext(final DSLContext jooq, final TimeoutSpec timeout) {
    this.jooq = jooq;
    this.pool = jooq.configuration().connectionProvider();
    this.timeout = timeout;
  }

  String dialect() {
    return jooq.configuration().dialect().getName();
  }

  TimeoutSpec timeout() {
    return timeout;
  }

  public Cursor<Record> query(final String sql) {
    try {
      init();
      connection.setReadOnly(true);
      final ResultSet rs = statement.executeQuery(sql);
      return jooq.fetchLazy(rs);
    } catch (SQLException e) {
      close();  // eager clean up after error
      throw new DatasetFailureException(e);
    }
  }

  public int update(final String sql) {
    try {
      init();
      connection.setReadOnly(false);
      return statement.executeUpdate(sql);
    } catch (SQLException e) {
      throw new DatasetFailureException(e);
    } finally {
      // eager cleanup
      close();
    }
  }

  private void init() throws SQLException {
    assert connection == null : "already executing";
    connection = pool.acquire();
    // FIXME : externalize rsType, rsConcurrency and rsHoldability settings
    statement = connection.createStatement();
    statement.setQueryTimeout((int) timeout.getAs(TimeUnit.SECONDS, 0L));
  }

  public void cancel() {
    try {
      if (statement != null) {
        statement.cancel();
      }
    } catch (SQLException e) {
      throw new DatasetFailureException(e);
    } finally {
      close();
    }
  }

  @Override
  public void close() {
    Resources.close(statement);
    if (connection != null) {
      pool.release(connection);
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("timeout", timeout)
        .add("dialect", jooq.configuration().dialect().getName())
        .toString();
  }
}
