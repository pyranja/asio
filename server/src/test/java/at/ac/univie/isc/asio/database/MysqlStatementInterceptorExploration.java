package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.sql.Database;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ResultSetInternalMethods;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.StatementInterceptorV2;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;

/**
 * Explore lifecycle and contract of mysql statement listeners.
 */
public class MysqlStatementInterceptorExploration {

  private final Database mysql =
      Database.create("jdbc:mysql:///public?statementInterceptors=" + DebugInterceptor.class.getName())
          .credentials("root", "change").build();

  @Before
  public void ensureMysqlAvailable() {
    assumeThat("cannot connect to mysql", mysql.isAvailable(), equalTo(true));
  }

  @Test
  public void single_query() throws Exception {
    mysql.execute("SHOW databases;");
  }

  @Test
  public void multiple_queries() throws Exception {
    try (final java.sql.Connection connection = mysql.datasource().getConnection()) {
      try (final java.sql.Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT * FROM person");
      }
      try (final java.sql.Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT * FROM datetimes");
      }
    }
  }

  @Test
  public void query_with_prepared_statement() throws Exception {
    try (final java.sql.Connection connection = mysql.datasource().getConnection()) {
      try (final java.sql.PreparedStatement statement = connection.prepareStatement("SELECT * FROM person")) {
        statement.execute();
      }
      try (final java.sql.PreparedStatement statement = connection.prepareStatement("SELECT * FROM person WHERE person.id = ?")) {
        statement.setInt(1, 1);
        statement.execute();
      }
    }
  }

  public static class DebugInterceptor implements StatementInterceptorV2 {
    public DebugInterceptor() {
      log("new DebugInterceptor()");
    }

    @Override
    public boolean executeTopLevelOnly() {
      log("executeTopLevelOnly()");
      return true;
    }

    @Override
    public void init(final Connection connection, final Properties properties) throws SQLException {
      log("init(%s, %s)", connection, properties);
    }

    @Override
    public ResultSetInternalMethods preProcess(final String sql, final Statement statement, final Connection connection) throws SQLException {
      log("preProcess(%s, %s, %s)", sql, statement, connection);
      return null;
    }

    @Override
    public ResultSetInternalMethods postProcess(final String s, final Statement statement, final ResultSetInternalMethods resultSetInternalMethods, final Connection connection, final int i, final boolean b, final boolean b1, final SQLException e) throws SQLException {
      log("postProcess(%s, %s, %s, %s, %d, %s, %s, %s", s, statement, resultSetInternalMethods, connection, i, b, b1, e);
      return null;
    }

    @Override
    public void destroy() {
      log("destroy()");
    }

    private void log(final String template, final Object... args) {
      System.out.printf(Locale.ENGLISH, template + "%n", args);
    }
  }
}
