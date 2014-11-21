package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.SqlSchema;
import com.google.common.base.Supplier;
import org.jooq.Catalog;
import org.jooq.DSLContext;
import org.jooq.Schema;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public final class H2SchemaProvider implements Supplier<SqlSchema> {
  public static class RepositoryFailure extends DatasetFailureException {
    public RepositoryFailure(final Throwable cause) {
      super("failed to fetch relational schema from database", cause);
    }
  }

  public static final Set<String> INTERNAL_SCHEMA = Collections.singleton("INFORMATION_SCHEMA");

  private final DataSource db;

  public H2SchemaProvider(final DataSource db) {
    this.db = db;
  }

  @Override
  public SqlSchema get() {
    final SqlSchemaBuilder builder = SqlSchemaBuilder.create();
    try (final Connection conn = db.getConnection()) {
      final DSLContext jooq = DSL.using(conn);
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
    } catch (SQLException e) {
      throw new RepositoryFailure(e);
    }
    return builder.build();
  }

  private boolean isNotExcluded(final Schema schema) {
    return schema.getName() != null
        && !INTERNAL_SCHEMA.contains(schema.getName().toUpperCase(Locale.ENGLISH));
  }
}
