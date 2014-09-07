package at.ac.univie.isc.asio.sql;

import com.google.common.base.Throwables;
import com.google.common.collect.Table;
import com.zaxxer.hikari.util.DriverDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Encapsulate access to an in-memory or remote database.
 */
public final class Database {
  private final DataSource datasource;

  public static Builder create(final String jdbcUrl) {
    return new Builder(jdbcUrl);
  }

  public Database(final DataSource datasource) {
    this.datasource = datasource;
  }

  public Table<Integer, String, String> reference(final String query) {
    try (
        final Connection connection = connection();
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(query)
    ) {
      return ConvertToTable.fromResultSet(resultSet);
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
  }

  public void execute(final String query) {
    try (
        final Connection connection = connection();
        final Statement statement = connection.createStatement();
    ) {
      statement.execute(query);
    } catch (SQLException e) {
      Throwables.propagate(e);
    }
  }

  private Connection connection() {
    try {
      return datasource.getConnection();
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
  }

  public static final class Builder {
    private final String jdbcUrl;
    private final Properties props;
    private String username;
    private String password;

    public Builder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      this.props = new Properties();
    }

    public Builder credentials(final String username, final String password) {
      this.username = username;
      this.password = password;
      return this;
    }

    public Database build() {
      final DriverDataSource pool = new DriverDataSource(jdbcUrl, props, username, password);
      return new Database(pool);
    }
  }
}
