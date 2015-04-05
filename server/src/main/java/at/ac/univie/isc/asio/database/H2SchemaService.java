package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.sql.SqlSchemaBuilder;
import org.jooq.Catalog;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Inspect H2 schema data. Note: H2 identifiers are always upper cased.
 */
public final class H2SchemaService implements RelationalSchemaService {

  /** H2 system schemas */
  public static final Set<String> INTERNAL_SCHEMA = Collections.singleton("INFORMATION_SCHEMA");

  private final DataSource pool;

  public H2SchemaService(final DataSource pool) {
    this.pool = pool;
  }

  public SqlSchema explore(final Id identifier) {
    final SqlSchemaBuilder builder = SqlSchemaBuilder.create();
    try (final Connection connection = pool.getConnection()) {
      final DSLContext jooq = DSL.using(connection, SQLDialect.H2);
      for (final Catalog sqlCatalog : jooq.meta().getCatalogs()) {
        builder.switchCatalog(sqlCatalog);
        for (final org.jooq.Schema sqlSchema : sqlCatalog.getSchemas()) {
          if (isNotExcluded(sqlSchema, identifier)) {
            builder.switchSchema(sqlSchema);
            for (final org.jooq.Table<?> sqlTable : sqlSchema.getTables()) {
              builder.add(sqlTable);
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage(), e);
    }
    return builder.build();
  }

  private boolean isNotExcluded(final org.jooq.Schema schema, final Id identifier) {
    final String name = schema.getName();
    return name != null
        && name.equalsIgnoreCase(identifier.asString())
        && !INTERNAL_SCHEMA.contains(name.toUpperCase(Locale.ENGLISH))
    ;
  }
}
