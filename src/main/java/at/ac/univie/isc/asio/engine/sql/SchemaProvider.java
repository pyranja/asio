package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetFailureException;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.jooq.*;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public final class SchemaProvider implements Supplier<SqlSchema> {
  public static class RepositoryFailure extends DatasetFailureException {
    public RepositoryFailure(final Throwable cause) {
      super("failed to fetch relational schema from database", cause);
    }
  }

  public static final String H2_INTERNAL_SCHEMA = "INFORMATION_SCHEMA";
  private static final ObjectFactory JAXB = new ObjectFactory();

  private final DataSource db;
  private final Set<String> excludedSchema;

  public SchemaProvider(final DataSource db, final String... excluded) {
    excludedSchema = ImmutableSet.copyOf(excluded);
    this.db = db;
  }

  @Override
  public SqlSchema get() {
    final SqlSchema schema = JAXB.createSqlSchema();
    try (final Connection conn = db.getConnection()) {
      final DSLContext jooq = DSL.using(conn);
      for (final Catalog sqlCatalog : jooq.meta().getCatalogs()) {
        for (final Schema sqlSchema : sqlCatalog.getSchemas()) {
          if (isNotExcluded(sqlSchema)) {
            for (final org.jooq.Table<?> sqlTable : sqlSchema.getTables()) {
              final Table metaTable = JAXB.createTable()
                  .withCatalog(sqlCatalog.getName())
                  .withSchema(sqlSchema.getName())
                  .withName(sqlTable.getName());
              attachColumns(sqlTable, metaTable);
              schema.getTable().add(metaTable);
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new RepositoryFailure(e);
    }
    return schema;
  }

  private boolean isNotExcluded(final Schema schema) {
    return !excludedSchema.contains(schema.getName());
  }

  private void attachColumns(final org.jooq.Table<?> sqlTable, final Table metaTable) {
    for (Field<?> field : sqlTable.fields()) {
      final DataType<?> sqlType = field.getDataType();
      final XmlSchemaType xmlType = XmlSchemaType.fromJavaType(sqlType.getType());
      metaTable.getColumn().add(JAXB.createColumn()
          .withName(field.getName())
          .withType(xmlType.qname())
          .withSqlType(sqlType.getTypeName())
          .withLength(sqlType.length()));
    }
  }
}
