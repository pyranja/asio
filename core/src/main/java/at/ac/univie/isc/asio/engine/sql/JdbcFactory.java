package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.tool.Resources;
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

  public JdbcSpec getSpec() {
    return spec;
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

  DSLContext jooq() {
    return DSL.using(pool, spec.getDialect());
  }

  public JooqEngine.SchemaProvider schemaProvider() {
    switch (spec.getDialect()) {
      case MYSQL:
        return new MySqlSchemaProvider(this);
      case H2:
        return new H2SchemaProvider(this);
      default:
        throw new IllegalStateException("sql dialect not supported "+ spec.getDialect());
    }
  }

  @Override
  public void close() {
    Resources.close(pool);
  }
}
