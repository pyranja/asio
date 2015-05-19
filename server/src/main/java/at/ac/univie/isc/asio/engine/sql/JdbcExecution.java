/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.tool.Closer;
import org.jooq.Cursor;
import org.jooq.Record;
import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manage a SQL execution.
 */
final class JdbcExecution implements AutoCloseable {
  private final JdbcFactory create;

  private final SQLExceptionTranslator translator;
  private Connection connection;
  private Statement statement;

  JdbcExecution(final JdbcFactory create) {
    this.create = create;
    translator = new SQLExceptionSubclassTranslator();
  }

  public Cursor<Record> query(final String sql) {
    try {
      init();
      connection.setReadOnly(true);
      final ResultSet rs = statement.executeQuery(sql);
      return create.lazy(rs);
    } catch (SQLException e) {
      close();  // eager clean up after error
      throw translator.translate("sql-query", sql, e);
    }
  }

  public int update(final String sql) {
    try {
      init();
      connection.setReadOnly(false);
      return statement.executeUpdate(sql);
    } catch (SQLException e) {
      throw translator.translate("sql-update", sql, e);
    } finally {
      close();  // eager cleanup
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
      throw new CleanupFailureDataAccessException("error when cancelling jdbc execution", e);
    } finally {
      close();
    }
  }

  @Override
  public void close() {
    Closer.quietly(statement);
    Closer.quietly(connection);
  }
}
