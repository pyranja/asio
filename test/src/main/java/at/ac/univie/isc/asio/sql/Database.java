package at.ac.univie.isc.asio.sql;

import com.google.common.base.Throwables;
import com.google.common.collect.Table;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Provide basic access to a fixed relational database through JDBC.
 */
@ThreadSafe
public final class Database {
  private final DriverManagerDataSource datasource;

  public static Builder create(final String jdbcUrl) {
    return new Builder(jdbcUrl);
  }

  private Database(@Nonnull final DriverManagerDataSource datasource) {
    this.datasource = datasource;
  }

  /**
   * @return pool of connections to the backing database
   */
  @Nonnull
  public DriverManagerDataSource datasource() {
    return datasource;
  }

  /**
   * Execute the given {@code SELECT} statement and convert the results to an in-memory representation.
   * The returned {@link com.google.common.collect.Table table's} row keys are the row number of the
   * result set, the column keys are the column names of the result set and the cell values are the
   * string values, as given by {@link java.sql.ResultSet#getString(int)}.
   *
   * @param query to be executed
   * @return the query results
   * @throws java.lang.RuntimeException wrapping any sql error
   */
  @Nonnull
  public Table<Integer, String, String> reference(@Nonnull final String query) {
    requireNonNull(query);
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

  /**
   * Execute the given {@code SQL} statement.
   *
   * @param query any valid SQL statement (including DDL and DML).
   * @throws java.lang.RuntimeException wrapping any error
   */
  public Database execute(@Nonnull final String query) {
    requireNonNull(query);
    try (
        final Connection connection = connection();
        final Statement statement = connection.createStatement();
    ) {
      statement.execute(query);
      return this;
    } catch (SQLException e) {
      throw Throwables.propagate(e);
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

    public Builder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      this.props = new Properties();
    }

    public Builder credentials(final String username, final String password) {
      props.setProperty("user", username);
      props.setProperty("password", password);
      return this;
    }

    public Database build() {
      final DriverManagerDataSource pool = new DriverManagerDataSource(jdbcUrl, props);
      return new Database(pool);
    }
  }
}
