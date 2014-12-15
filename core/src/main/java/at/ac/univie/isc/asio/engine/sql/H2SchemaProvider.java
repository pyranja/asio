package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.SqlSchema;
import com.google.common.base.Supplier;
import org.jooq.Catalog;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public final class H2SchemaProvider implements Supplier<SqlSchema>, JooqEngine.SchemaProvider {
  public static class RepositoryFailure extends DatasetFailureException {
    public RepositoryFailure(final Throwable cause) {
      super("failed to fetch relational schema from database", cause);
    }
  }

  public static final Set<String> INTERNAL_SCHEMA = Collections.singleton("INFORMATION_SCHEMA");

  private final JdbcFactory<?> create;

  public H2SchemaProvider(final JdbcFactory<?> create) {
    this.create = create;
  }

  @Override
  public SqlSchema fetch() {
    final SqlSchemaBuilder builder = SqlSchemaBuilder.create();
    try (final Connection connection = create.connection()) {
      final DSLContext jooq = DSL.using(connection, SQLDialect.H2);
      for (final Catalog sqlCatalog : jooq.meta().getCatalogs()) {
        builder.switchCatalog(sqlCatalog);
        for (final Schema sqlSchema : sqlCatalog.getSchemas()) {
          if (isNotExcluded(sqlSchema)) {
            builder.switchSchema(sqlSchema);
            for (final org.jooq.Table<?> sqlTable : sqlSchema.getTables()) {
              builder.add(sqlTable);
            }
          }
        }
      }
    } catch (SQLException | DataAccessException e) {
      throw new RepositoryFailure(e);
    }
    return builder.build();
  }

  private boolean isNotExcluded(final Schema schema) {
    return schema.getName() != null
        && !INTERNAL_SCHEMA.contains(schema.getName().toUpperCase(Locale.ENGLISH));
  }

  @Override
  public SqlSchema get() {
    return fetch();
  }
}
