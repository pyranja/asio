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
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Create and parametrize JDBC connections and statements.
 */
final class JdbcFactory<POOL extends DataSource & AutoCloseable> implements AutoCloseable {
  private final POOL pool;
  private final JdbcSpec spec;
  private final DSLContext jooq;

  JdbcFactory(final POOL pool, final JdbcSpec spec) {
    this.pool = requireNonNull(pool);
    this.spec = requireNonNull(spec);
    jooq = DSL.using(pool, spec.getDialect());
  }

  public Connection connection() throws SQLException {
    return pool.getConnection();
  }

  public Statement statement(final Connection connection) throws SQLException {
    final Statement statement = connection.createStatement();
    statement.setQueryTimeout((int) spec.getTimeout().getAs(TimeUnit.SECONDS, 0L));
    return statement;
  }

  public Cursor<Record> lazy(final ResultSet rs) {
    return jooq.fetchLazy(rs);
  }

  @Override
  public void close() {
    Closer.quietly(pool);
  }
}
