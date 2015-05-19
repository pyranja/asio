/*
 * #%L
 * asio test
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.sql;

import com.google.common.base.Throwables;
import com.google.common.collect.Table;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Provide basic access to a fixed relational database through JDBC.
 */
@ThreadSafe
public final class Database {
  private final DriverManagerDataSource datasource;
  private String catalog;

  public static Builder create(final String jdbcUrl) {
    return new Builder(jdbcUrl);
  }

  private Database(@Nonnull final DriverManagerDataSource datasource) {
    this.datasource = datasource;
  }

  public Database switchCatalog(final String catalog) {
    this.catalog = catalog;
    return this;
  }

  /**
   * @return pool of connections to the backing database
   */
  @Nonnull
  public DriverManagerDataSource datasource() {
    return datasource;
  }

  private Connection connect() throws SQLException {
    final Connection connection = datasource.getConnection();
    if (catalog != null) { connection.setCatalog(catalog); }
    return connection;
  }

  /**
   * Tests whether a valid connection can be obtained from the backing datasource.
   *
   * @return true if database seems to be reachable
   */
  public boolean isAvailable() {
    try (final Connection connection = connect()) {
      return connection.isValid(5);
    } catch (SQLException ignored) {
      return false;
    }
  }

  /**
   * Name of the database product, e.g. {@code mysql}.
   *
   * @return lower case name of the database
   */
  public String getType() {
    try (final Connection connection = connect()) {
      return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ENGLISH);
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
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
        final Connection connection = connect();
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
        final Connection connection = connect();
        final Statement statement = connection.createStatement();
    ) {
      statement.execute(query);
      return this;
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
  }

  public static final class Builder {
    private final String jdbcUrl;
    private final Properties props;

    private Builder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      this.props = new Properties();
    }

    /** set the database authentication credentials */
    public Builder credentials(final String username, final String password) {
      props.setProperty("user", username);
      props.setProperty("password", password);
      return this;
    }

    /** finish building the database */
    public Database build() {
      final DriverManagerDataSource pool = new DriverManagerDataSource(jdbcUrl, props);
      return new Database(pool);
    }
  }
}
