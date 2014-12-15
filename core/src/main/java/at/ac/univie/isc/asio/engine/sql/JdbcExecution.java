package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.tool.Resources;
import org.jooq.Cursor;
import org.jooq.Record;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manage a SQL execution.
 */
final class JdbcExecution implements AutoCloseable {
  private final JdbcFactory create;

  private Connection connection;
  private Statement statement;

  JdbcExecution(final JdbcFactory create) {
    this.create = create;
  }

  public Cursor<Record> query(final String sql) {
    try {
      init();
      connection.setReadOnly(true);
      final ResultSet rs = statement.executeQuery(sql);
      return create.lazy(rs);
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
    connection = create.connection();
    statement = create.statement(connection);
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
    Resources.close(connection);
  }
}
