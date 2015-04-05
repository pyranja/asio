package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.ObjectFactory;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import org.jooq.Catalog;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Schema;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulate transformation from Jooq model classes to Asio JAXB representations.
 */
public final class SqlSchemaBuilder {
  private static final ObjectFactory JAXB = new ObjectFactory();

  public static SqlSchemaBuilder create() {
    return new SqlSchemaBuilder();
  }

  private final SqlSchema product;
  private String activeCatalog;
  private String activeSchema;

  private SqlSchemaBuilder() {
    product = JAXB.createSqlSchema();
  }

  public SqlSchemaBuilder switchCatalog(final Catalog catalog) {
    this.activeCatalog = requireNonNull(catalog.getName());
    return this;
  }

  public SqlSchemaBuilder noCatalog() {
    this.activeCatalog = null;
    return this;
  }

  public SqlSchemaBuilder switchSchema(final Schema schema) {
    this.activeSchema = requireNonNull(schema.getName());
    return this;
  }

  public SqlSchemaBuilder noSchema() {
    this.activeSchema = null;
    return this;
  }

  public SqlSchemaBuilder add(final org.jooq.Table<?> sqlTable) {
    final Table metaTable = JAXB.createTable()
        .withCatalog(activeCatalog)
        .withSchema(activeSchema)
        .withName(sqlTable.getName())
        ;
    for (Field<?> field : sqlTable.fields()) {
      final DataType<?> sqlType = field.getDataType();
      final XmlSchemaType xmlType = XmlSchemaType.fromJavaType(sqlType.getType());
      metaTable.getColumn().add(JAXB.createColumn()
          .withName(field.getName())
          .withType(xmlType.qname())
          .withSqlType(sqlType.getTypeName())
          .withLength(sqlType.length()));
    }
    product.getTable().add(metaTable);
    return this;
  }

  public SqlSchema build() {
    return product;
  }
}
