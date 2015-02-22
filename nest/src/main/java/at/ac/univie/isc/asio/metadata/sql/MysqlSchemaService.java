package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.SchemaIdentifier;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.sql.SqlSchemaBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

/**
 * Inspect MySQL database metadata. Note: A {@code schema} is called {@code database} in MySQL.
 * This service implementation accesses the database via a fixed connection pool, but changes
 * the database to the requested one before exploring.
 */
public final class MysqlSchemaService implements RelationalSchemaService {

  /** MySQL system schemas */
  public static final Set<String> INTERNAL_SCHEMA =
      ImmutableSet.of("mysql", "information_schema", "performance_schema");

  private final DataSource pool;

  public MysqlSchemaService(final DataSource pool) {
    this.pool = pool;
  }

  @Override
  public SqlSchema explore(final SchemaIdentifier target) throws SchemaNotFound {
    final SqlSchemaBuilder builder = SqlSchemaBuilder.create().noCatalog();
    try (final Connection connection = pool.getConnection()) {
      final DSLContext jooq = DSL.using(connection, SQLDialect.MYSQL);
      final Schema schema = findActiveSchema(jooq, target);
      builder.switchSchema(schema);
      for (final org.jooq.Table<?> sqlTable : schema.getTables()) {
        builder.add(sqlTable);
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage(), e);
    }
    return builder.build();
  }

  private Schema findActiveSchema(final DSLContext jooq, final SchemaIdentifier target) throws SQLException {
    final String activeSchemaName = target.name();
    if (INTERNAL_SCHEMA.contains(activeSchemaName.toLowerCase(Locale.ENGLISH))) {
      throw new SchemaNotFound(target, "cannot access mysql system schemas");
    }
    final Schema schema = Iterables.getOnlyElement(jooq.meta().getCatalogs()).getSchema(activeSchemaName);
    if (schema == null) {
      throw new SchemaNotFound(target, "not available in catalog");
    }
    return schema;
  }
}
